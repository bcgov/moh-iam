import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.*;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.ProcessingException;

import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class KeycloakBulkClientRoleAdder {

    private static boolean SIMULATION_MODE = true;
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String CYAN = "\u001B[36m";

    public static void main(String[] args) {
        String inputFilePath = null;
        String envArg = null;
        String realm = null;
        String clientId = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--input") && i + 1 < args.length) {
                inputFilePath = args[i + 1];
            }
            if (args[i].equalsIgnoreCase("--env") && i + 1 < args.length) {
                envArg = args[i + 1].toUpperCase();
            }
            if (args[i].equalsIgnoreCase("--realm") && i + 1 < args.length) {
                realm = args[i + 1];
            }
            if (args[i].equalsIgnoreCase("--client") && i + 1 < args.length) {
                clientId = args[i + 1];
            }
            if (args[i].equalsIgnoreCase("--real")) {
                SIMULATION_MODE = false;
            }
        }

        if (inputFilePath == null || envArg == null || realm == null || clientId == null) {
            System.err.println("Usage: java KeycloakBulkClientRoleAdder --input file.csv --env DEV|TEST|PROD --realm REALM_NAME --client CLIENT_ID [--real]");
            System.exit(1);
        }

        EnvConfig targetEnv;
        try {
            targetEnv = EnvConfig.valueOf(envArg);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid environment: " + envArg);
            System.err.println("Valid values are: DEV, TEST, PROD");
            System.exit(1);
            return;
        }

        // ===== Early secret check (no network hit yet) =====
        if (targetEnv.clientSecret.isEmpty()) {
            System.err.println(RED + "Missing client secret for environment " + targetEnv.name() + RESET);
            System.err.println("Expected env var name: " + CYAN + targetEnv.clientId + RESET);
            System.err.println("Set it and re-run. Example (bash):");
            System.err.println("  export " + targetEnv.clientId + "='<secret-value>'");
            System.exit(1);
        }

        List<UserRoleEntry> entries = loadUserRoleEntries(inputFilePath);
        setupLoggingToFile();

        System.out.println("Running in " + (SIMULATION_MODE ? GREEN + "SIMULATION" : RED + "REAL") + RESET + " mode.");
        System.out.println("Target environment: " + CYAN + targetEnv.name() + RESET);
        System.out.println("Loaded " + entries.size() + " user/role entries.");

        printEnvironmentBanner(targetEnv.url);

        if (!SIMULATION_MODE && targetEnv.isProd()) {
            System.out.print(RED + "You are running in PRODUCTION and REAL mode. Type 'I UNDERSTAND' to continue: " + RESET);
            Scanner scanner = new Scanner(System.in);
            String confirm = scanner.nextLine().trim();
            if (!confirm.equalsIgnoreCase("I UNDERSTAND")) {
                System.out.println("Aborted.");
                System.exit(1);
            }
        }

        Keycloak keycloak = authenticate(targetEnv);

        // ===== Auth + permission preflight (fast failure with helpful hints) =====
        preflight(keycloak, realm, clientId);

        String clientUuid = getClientUuid(keycloak, realm, clientId);
        if (clientUuid == null) {
            System.err.println(RED + "Client not found: " + clientId + RESET);
            System.exit(1);
        }

        // Build role cache
        Map<String, RoleRepresentation> clientRoleMap = loadClientRoleMap(keycloak, realm, clientUuid);

        // Verify roles exist
        preflightRoles(collectRequestedRoleNames(entries), clientRoleMap, clientId);

        for (UserRoleEntry entry : entries) {
            try {
                List<UserRepresentation> found = keycloak.realm(realm).users().search(entry.username, true);
                UserRepresentation user = found.stream()
                        .filter(u -> entry.username.equalsIgnoreCase(u.getUsername()))
                        .findFirst()
                        .orElse(null);

                if (user == null) {
                    System.err.printf("User not found: %s%n", entry.username);
                    continue;
                }

                System.out.printf("\nUser: %s (%s)%n", user.getUsername(), user.getId());

                UserResource userRes = keycloak.realm(realm).users().get(user.getId());
                List<RoleRepresentation> existingRoles = userRes.roles().clientLevel(clientUuid).listEffective();

                Set<String> existingRoleNames = existingRoles.stream()
                        .map(RoleRepresentation::getName)
                        .collect(Collectors.toSet());

                List<RoleRepresentation> rolesToAdd = new ArrayList<>();
                for (String roleName : entry.roles) {
                    RoleRepresentation role = clientRoleMap.get(roleName);

                    if (!existingRoleNames.contains(roleName)) {
                        rolesToAdd.add(role);
                    } else {
                        System.out.printf("Role already assigned: %s%n", roleName);
                    }
                }

                if (rolesToAdd.isEmpty()) {
                    System.out.println("No new roles to add.");
                } else if (SIMULATION_MODE) {
                    System.out.printf("[SIMULATION] Would add roles %s to user %s%n",
                            rolesToAdd.stream().map(RoleRepresentation::getName).collect(Collectors.joining(", ")),
                            user.getUsername());
                } else {
                    userRes.roles().clientLevel(clientUuid).add(rolesToAdd);
                    System.out.printf("[REAL] Added roles %s to user %s%n",
                            rolesToAdd.stream().map(RoleRepresentation::getName).collect(Collectors.joining(", ")),
                            user.getUsername());
                }

            } catch (Exception e) {
                System.err.printf("Error processing user %s: %s%n", entry.username, e.getMessage());
                e.printStackTrace();
            }
        }

        keycloak.close();
        System.out.println("\nProcessed " + entries.size() + " entries in " + targetEnv.name() + ".");
        System.out.println("Done.");
    }

    // --- Preflight auth + perms ---
    static void preflight(Keycloak keycloak, String realm, String clientId) {
        System.out.println(CYAN + "Preflight: verifying credentials and permissions..." + RESET);
        try {
            // Force token retrieval to catch 401 immediately
            keycloak.tokenManager().grantToken();
        } catch (ProcessingException | NotAuthorizedException e) {
            System.err.println(RED + "Authentication failed (401 Unauthorized)." + RESET);
            System.err.println("Likely causes:");
            System.err.println(" - Wrong client secret");
            System.err.println(" - Client disabled or not in realm '" + realm + "'");
            System.err.println(" - Using the wrong server URL/realm for these credentials");
            throw e;
        }

        try {
            // Minimal permission probe: requires at least view-users (or query-users) on realm-management
            keycloak.realm(realm).users().count();
        } catch (ForbiddenException e) {
            System.err.println(RED + "Authenticated, but missing permissions (403 Forbidden) to list users." + RESET);
            System.err.println("Service account likely needs realm-management roles such as:");
            System.err.println(" - view-users (to search/count users)");
            System.err.println(" - manage-users (to assign roles)");
            System.err.println(" - view-clients or query-clients (to read client + roles)");
            throw e;
        }

        try {
            // Quick existence check for the target client; if missing permissions, this can also 403
            List<ClientRepresentation> clients = keycloak.realm(realm).clients().findByClientId(clientId);
            if (clients.isEmpty()) {
                System.err.println(RED + "Client with clientId '" + clientId + "' not found in realm '" + realm + "'." + RESET);
                System.err.println("Double-check the --client value, realm, and your account's client view permissions.");
                System.exit(1);
            }
        } catch (ForbiddenException e) {
            System.err.println(RED + "Authenticated, but missing permissions (403 Forbidden) to view clients." + RESET);
            System.err.println("Service account likely needs: view-clients or query-clients on realm-management.");
            throw e;
        }

        System.out.println(GREEN + "Preflight OK." + RESET);
    }

    static String getClientUuid(Keycloak keycloak, String realm, String clientId) {
        List<ClientRepresentation> clients = keycloak.realm(realm).clients().findByClientId(clientId);
        if (clients.isEmpty()) return null;
        return clients.get(0).getId();
    }

    static List<UserRoleEntry> loadUserRoleEntries(String path) {
        List<UserRoleEntry> entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = stripBOM(line).trim();
                if (trimmed.isEmpty()) continue;
                String[] parts = trimmed.split(",", 2);
                if (parts.length < 2) continue;
                // TODO, let's have error handling to check parts == 2 else error. The file should be perfect.

                String username = parts[0].trim();
                String rolesPart = parts[1].replaceAll("^\"|\"$", "").trim();
                List<String> roles = Arrays.stream(rolesPart.split(","))
                        .map(String::trim)
                        .filter(r -> !r.isEmpty())
                        .collect(Collectors.toList());

                entries.add(new UserRoleEntry(username, roles));
            }
        } catch (IOException e) {
            System.err.println("Failed to read input file: " + e.getMessage());
            System.exit(1);
        }
        return entries;
    }

    static String stripBOM(String s) {
        if (s != null && !s.isEmpty() && s.charAt(0) == '\ufeff') {
            return s.substring(1);
        }
        return s;
    }

    static class UserRoleEntry {
        String username;
        List<String> roles;
        UserRoleEntry(String username, List<String> roles) {
            this.username = username;
            this.roles = roles;
        }
    }

    enum EnvConfig {
        DEV("https://common-logon-dev.hlth.gov.bc.ca/auth", "admin-safety-toggle-dev"),
        TEST("https://common-logon-test.hlth.gov.bc.ca/auth", "immsbc-bulk-load-serviceaccount"),
        PROD("https://common-logon.hlth.gov.bc.ca/auth", "admin-safety-toggle-prod");

        public final String url;
        public final String clientId;   // env var name
        public final String clientSecret;

        EnvConfig(String url, String clientId) {
            this.url = url;
            this.clientId = clientId;
            this.clientSecret = System.getenv(clientId);
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
        String logFileName = "KeycloakBulkClientRoleAdder_" + timestamp + ".log";

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

    static Map<String, RoleRepresentation> loadClientRoleMap(Keycloak keycloak, String realm, String clientUuid) {
        List<RoleRepresentation> roleList = keycloak.realm(realm).clients().get(clientUuid).roles().list();
        Map<String, RoleRepresentation> map = new HashMap<>(roleList.size());
        for (RoleRepresentation rr : roleList) {
            map.put(rr.getName(), rr); // keep case sensitivity; KC role names are case-sensitive
            // TODO: Are Keycloak roles actually case-senstive?
        }
        return map;
    }

    static void preflightRoles(Set<String> requestedRoleNames, Map<String, RoleRepresentation> clientRoleMap, String clientId) {
        List<String> missing = requestedRoleNames.stream()
                .filter(r -> !clientRoleMap.containsKey(r))
                .sorted()
                .collect(Collectors.toList());
        if (!missing.isEmpty()) {
            System.err.println(RED + "Missing roles on client '" + clientId + "':" + RESET);
            missing.forEach(r -> System.err.println("  - " + r));
            System.err.println("Create these roles on the client or fix the input file, then re-run.");
            System.exit(1);
        }
    }

    static Set<String> collectRequestedRoleNames(List<UserRoleEntry> entries) {
        return entries.stream()
                .flatMap(e -> e.roles.stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
