import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class KeycloakUserDisabler {

    private static boolean SIMULATION_MODE = true;
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String CYAN = "\u001B[36m";

    public static void main(String[] args) {
        String inputFilePath = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--input") && i + 1 < args.length) {
                inputFilePath = args[i + 1];
            }
            if (args[i].equalsIgnoreCase("--real")) {
                SIMULATION_MODE = false;
            }
        }

        if (inputFilePath == null) {
            System.err.println("Usage: java KeycloakUserDisabler --input users.txt [--real]");
            System.exit(1);
        }

        List<List<String>> userSearchBlocks = loadSearchBlocksFromFile(inputFilePath);
        setupLoggingToFile();

        System.out.println("Running in " + (SIMULATION_MODE ? GREEN + "SIMULATION" : RED + "REAL") + RESET + " mode.");
        System.out.println("Loaded " + userSearchBlocks.size() + " user blocks from file.");

        List<EnvConfig> envs = Arrays.asList(EnvConfig.values());

        Scanner scanner = new Scanner(System.in);
        boolean prodConfirmed = false;
        Set<String> allProcessedUserIds = new HashSet<>();

        for (List<String> searchTerms : userSearchBlocks) {
            System.out.println("\n=== Processing next user block: " + searchTerms + " ===");

            for (EnvConfig env : envs) {
                printEnvironmentBanner(env.url);

                if (!SIMULATION_MODE && env.isProd() && !prodConfirmed) {
                    System.out.print(RED + "You are running in PRODUCTION and REAL mode. Type 'I UNDERSTAND' to continue: " + RESET);
                    String confirm = scanner.nextLine().trim();
                    if (!confirm.equalsIgnoreCase("I UNDERSTAND")) {
                        System.out.println("Aborted.");
                        System.exit(1);
                    }
                    prodConfirmed = true;
                }

                Keycloak keycloak = authenticate(env);

                for (String realm : getAllRealmNames(keycloak)) {
                    try {
                        for (String term : searchTerms) {
                            List<UserRepresentation> users = keycloak.realm(realm).users().search(term, true, 0, 50);

                            for (UserRepresentation user : users) {
                                if (!matches(user, term)) continue;

                                String key = env.name() + ":" + realm + ":" + user.getId();
                                if (allProcessedUserIds.contains(key)) continue;
                                allProcessedUserIds.add(key);

                                System.out.printf("\nMatch Found in %s / %s:\n", env.name(), realm);
                                System.out.printf("Username: %s\nEmail: %s\nFirst Name: %s\nLast Name: %s\nUser ID: %s\n",
                                        user.getUsername(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getId());

                                System.out.print("Disable this user? (yes/no): ");
                                String confirm = scanner.nextLine().trim().toLowerCase();
                                System.out.println(confirm); // echo for logs

                                if (confirm.equals("yes")) {
                                    disableUser(keycloak, realm, user.getId(), user.getUsername(), env.name());
                                } else {
                                    System.out.println("Skipped.");
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.printf("Error searching in realm %s: %s%n", realm, e.getMessage());
                    }
                }

                keycloak.close();
            }
        }

        System.out.println("\nProcessed " + allProcessedUserIds.size() + " unique user entries across environments.");
        System.out.println("Done.");
    }

    static List<List<String>> loadSearchBlocksFromFile(String path) {
        List<List<String>> blocks = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> terms = Arrays.stream(line.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                if (!terms.isEmpty()) {
                    blocks.add(terms);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read input file: " + e.getMessage());
            System.exit(1);
        }
        return blocks;
    }

    enum EnvConfig {
        DEV("https://common-logon-dev.hlth.gov.bc.ca/auth", "svc-keycloak-cli-offboarder-dev"),
        TEST("https://common-logon-test.hlth.gov.bc.ca/auth", "svc-keycloak-cli-offboarder-test"),
        PROD("https://common-logon.hlth.gov.bc.ca/auth", "svc-keycloak-cli-offboarder-prod");

        public final String url;
        public final String clientId;
        public final String clientSecret;

        EnvConfig(String url, String clientId) {
            this.url = url;
            this.clientId = clientId;
            this.clientSecret = System.getenv(clientId); // load on init
        }

        public boolean isProd() {
            return this == PROD;
        }
    }

    static Keycloak authenticate(EnvConfig config) {
        return KeycloakBuilder.builder()
                .serverUrl(config.url)
                .realm("master")
                .clientId(config.clientId)
                .clientSecret(config.clientSecret)
                .grantType("client_credentials")
                .build();
    }

    static void disableUser(Keycloak keycloak, String realm, String userId, String username, String envName) {
        try {
            UserResource userRes = keycloak.realm(realm).users().get(userId);
            UserRepresentation user = userRes.toRepresentation();
            if (!user.isEnabled()) {
                System.out.println("User is already disabled.");
                return;
            }

            if (SIMULATION_MODE) {
                System.out.printf("[SIMULATION] Would disable user %s (%s) in realm %s (%s)%n", username, userId, realm, envName);
            } else {
                user.setEnabled(false);
                userRes.update(user);
                System.out.printf("[REAL] Disabled user %s (%s) in realm %s (%s)%n", username, userId, realm, envName);
            }
        } catch (Exception e) {
            System.err.printf("Failed to disable user %s in realm %s: %s%n", userId, realm, e.getMessage());
        }
    }

    static List<String> getAllRealmNames(Keycloak keycloak) {
        try {
            return keycloak.realms().findAll().stream()
                    .map(r -> r.getRealm())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Failed to retrieve realm list: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    static boolean matches(UserRepresentation user, String term) {
        String lowerTerm = term.toLowerCase();
        return Optional.ofNullable(user.getUsername()).orElse("").toLowerCase().contains(lowerTerm)
                || Optional.ofNullable(user.getEmail()).orElse("").toLowerCase().contains(lowerTerm)
                || Optional.ofNullable(user.getFirstName()).orElse("").toLowerCase().contains(lowerTerm)
                || Optional.ofNullable(user.getLastName()).orElse("").toLowerCase().contains(lowerTerm)
                || (Optional.ofNullable(user.getFirstName()).orElse("") + " " + Optional.ofNullable(user.getLastName()).orElse(""))
                .toLowerCase().contains(lowerTerm);
    }

    public static void setupLoggingToFile() {
        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        String logFileName = "KeycloakUserDisabler_" + timestamp + ".log";

        try {
            PrintStream originalOut = System.out;
            PrintStream fileOut = new PrintStream(new FileOutputStream(logFileName));

            PrintStream dualOut = new PrintStream(new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    originalOut.write(b);
                    fileOut.write(b);
                }

                @Override
                public void flush() throws IOException {
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
