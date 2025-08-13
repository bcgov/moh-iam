import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;

import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

/**
 * KeycloakIdpUnlinker is a standalone utility script for safely removing
 * incorrect PHSA_AAD identity provider links from user accounts in Keycloak.
 *
 * This program is used during remediation of a historical misconfiguration
 * where Keycloak accounts were created using email-based usernames instead
 * of the correct UPN-based values. These incorrect accounts can block users
 * from signing in to Panorama using their Health Authority ID.
 *
 * The script takes a list of environment-specific user IDs and removes the
 * PHSA_AAD identity provider link for each, allowing the correct account
 * to be linked or created on next login.
 *
 * Usage and environment targeting are controlled via command line options.
 * See README.md for full instructions and safety guidance.
 */
public class KeycloakIdpUnlinker {

    private static boolean SIMULATION_MODE = true;
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String CYAN = "\u001B[36m";

    public static void main(String[] args) {
        String inputFilePath = null;
        String envArg = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--input") && i + 1 < args.length) {
                inputFilePath = args[i + 1];
            }
            if (args[i].equalsIgnoreCase("--env") && i + 1 < args.length) {
                envArg = args[i + 1].toUpperCase();
            }
            if (args[i].equalsIgnoreCase("--real")) {
                SIMULATION_MODE = false;
            }
        }

        if (inputFilePath == null || envArg == null) {
            System.err.println("Usage: java KeycloakIdpUnlinker --input user_ids.txt --env DEV|TEST|PROD [--real]");
            System.exit(1);
        }

        EnvConfig targetEnv;
        try {
            targetEnv = EnvConfig.valueOf(envArg);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid environment: " + envArg);
            System.err.println("Valid values are: DEV, TEST, PROD");
            System.exit(1);
            return; // unreachable but silences IDE warnings
        }

        List<String> userIds = loadUserIds(inputFilePath);
        setupLoggingToFile();

        System.out.println("Running in " + (SIMULATION_MODE ? GREEN + "SIMULATION" : RED + "REAL") + RESET + " mode.");
        System.out.println("Target environment: " + CYAN + targetEnv.name() + RESET);
        System.out.println("Loaded " + userIds.size() + " user IDs.");

        Scanner scanner = new Scanner(System.in);
        Set<String> processed = new HashSet<>();

        printEnvironmentBanner(targetEnv.url);

        if (!SIMULATION_MODE && targetEnv.isProd()) {
            System.out.print(RED + "You are running in PRODUCTION and REAL mode. Type 'I UNDERSTAND' to continue: " + RESET);
            String confirm = scanner.nextLine().trim();
            if (!confirm.equalsIgnoreCase("I UNDERSTAND")) {
                System.out.println("Aborted.");
                System.exit(1);
            }
        }

        Keycloak keycloak = authenticate(targetEnv);
        String realm = "moh_applications";

        for (String userId : userIds) {
            String key = targetEnv.name() + ":" + realm + ":" + userId;
            if (processed.contains(key)) continue;
            processed.add(key);

            try {
                UserResource user = keycloak.realm(realm).users().get(userId);
                UserRepresentation rep = user.toRepresentation();

                System.out.printf("\nUser ID: %s\nUsername: %s\nEmail: %s\n",
                        rep.getId(), rep.getUsername(), rep.getEmail());

                if (SIMULATION_MODE) {
                    System.out.printf("[SIMULATION] Would remove PHSA_AAD link from user %s (%s)\n", rep.getUsername(), rep.getId());
                } else {
                    user.removeFederatedIdentity("phsa_aad");
                    System.out.printf("[REAL] Removed PHSA_AAD link from user %s (%s)\n", rep.getUsername(), rep.getId());
                }

            } catch (jakarta.ws.rs.NotFoundException e) {
                System.err.printf("User not found: %s\n", userId);
            } catch (Exception e) {
                System.err.printf("Error removing IdP link for user %s: %s\n", userId, e.getMessage());
                e.printStackTrace();
            }
        }

        keycloak.close();

        System.out.println("\nProcessed " + processed.size() + " user IDs in " + targetEnv.name() + ".");
        System.out.println("Done.");
    }

    static List<String> loadUserIds(String path) {
        List<String> ids = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    ids.add(trimmed);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read input file: " + e.getMessage());
            System.exit(1);
        }
        return ids;
    }

    enum EnvConfig {
        DEV("https://common-logon-dev.hlth.gov.bc.ca/auth", "svc-keycloak-cli-RFC-20250811"),
        TEST("https://common-logon-test.hlth.gov.bc.ca/auth", "svc-keycloak-cli-offboarder-test"),
        PROD("https://common-logon.hlth.gov.bc.ca/auth", "svc-keycloak-cli-offboarder-prod");

        public final String url;
        public final String clientId;
        public final String clientSecret;

        EnvConfig(String url, String clientId) {
            this.url = url;
            this.clientId = clientId;
            this.clientSecret = System.getenv(clientId); // loaded from env var
        }

        public boolean isProd() {
            return this == PROD;
        }
    }

    static Keycloak authenticate(EnvConfig config) {
        return KeycloakBuilder.builder()
                .serverUrl(config.url)
                .realm("moh_applications")
                .clientId(config.clientId)
                .clientSecret(config.clientSecret)
                .grantType("client_credentials")
                .build();
    }

    public static void setupLoggingToFile() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String logFileName = "KeycloakIdpUnlinker_" + timestamp + ".log";

        try {
            PrintStream originalOut = System.out;
            PrintStream fileOut = new PrintStream(new FileOutputStream(logFileName));

            PrintStream dualOut = new PrintStream(new OutputStream() {
                @Override
                public void write(int b) {
                    originalOut.write(b);
                    fileOut.write(b);
                }

                @Override
                public void flush() {
                    originalOut.flush();
                    fileOut.flush();
                }
            }, true);

            System.setOut(dualOut);
            System.setErr(dualOut);
            System.out.println("Logging to " + logFileName);
        } catch (IOException e) {
            System.err.println("Failed to set up log file: " + e.getMessage());
        }
    }

    static void printEnvironmentBanner(String url) {
        String urlLower = url.toLowerCase();
        boolean isProd = !(urlLower.contains("dev") || urlLower.contains("test"));

        String banner = "\n==============================\n" +
                "  ENVIRONMENT: " + CYAN + url.toUpperCase() + RESET + "\n" +
                (isProd
                        ? RED + "  ⚠️ PRODUCTION ENVIRONMENT — BE CAREFUL! ⚠️" + RESET + "\n"
                        : GREEN + "  (Non-prod environment)" + RESET + "\n") +
                "==============================";

        if (isProd) {
            Toolkit.getDefaultToolkit().beep();
        }

        System.out.println(banner);
    }
}
