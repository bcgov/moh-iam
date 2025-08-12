import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;

import java.awt.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class KeycloakUserDisablerBackup {

    private static final String KEYCLOAK_URL = "https://common-logon.hlth.gov.bc.ca/auth";
    private static final String CLIENT_ID = "svc-keycloak-cli-offboarder-prod";
    private static final String CLIENT_SECRET = System.getenv("svc-keycloak-cli-offboarder-prod");

//    private static final String KEYCLOAK_URL = "https://common-logon-test.hlth.gov.bc.ca/auth";
//    private static final String CLIENT_ID = "svc-keycloak-cli-offboarder-test";
//    private static final String CLIENT_SECRET = System.getenv("svc-keycloak-cli-offboarder-test");

//    private static final String KEYCLOAK_URL = "https://common-logon-dev.hlth.gov.bc.ca/auth";
//    private static final String CLIENT_ID = "svc-keycloak-cli-offboarder-dev";
//    private static final String CLIENT_SECRET = System.getenv("svc-keycloak-cli-offboarder-dev");

    private static boolean SIMULATION_MODE = true;

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("real")) {
            SIMULATION_MODE = false;
        }
        setupLoggingToFile();
        printEnvironmentBanner(KEYCLOAK_URL);

        if (isProdEnvironment(KEYCLOAK_URL) && SIMULATION_MODE == false) {
            Scanner confirmScanner = new Scanner(System.in);
            System.out.print(RED + "You are running in PRODUCTION and REAL mode. Type 'I UNDERSTAND' to continue: " + RESET);
            String confirm = confirmScanner.nextLine().trim();
            if (!confirm.equalsIgnoreCase("I UNDERSTAND")) {
                System.out.println("Aborted.");
                System.exit(1);
            }
        }

        System.out.println("Running in " + (SIMULATION_MODE ? GREEN + "SIMULATION" : RED + "REAL") + RESET + " mode.");

        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter search terms (comma-separated): ");
        String rawInput = scanner.nextLine().trim();
        Set<String> searchTerms = Arrays.stream(rawInput.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        System.out.println("Search terms: " + searchTerms);

        Keycloak keycloak = authenticateWithKeycloak();

        Set<String> allProcessedUserIds = new HashSet<>();

        for (String realm : getAllRealmNames(keycloak)) {
            try {
                for (String term : searchTerms) {
                    List<UserRepresentation> users = keycloak.realm(realm).users().search(term, true,0, 50);

                    for (UserRepresentation user : users) {
                        if (!matches(user, term)) continue;
                        if (allProcessedUserIds.contains(user.getId())) continue; // skip duplicates

                        allProcessedUserIds.add(user.getId());

                        System.out.println("\nMatch Found:");
                        System.out.printf("Realm: %s%n", realm);
                        System.out.printf("Username: %s%n", user.getUsername());
                        System.out.printf("Email: %s%n", user.getEmail());
                        System.out.printf("First Name: %s%n", user.getFirstName());
                        System.out.printf("Last Name: %s%n", user.getLastName());
                        System.out.printf("User ID: %s%n", user.getId());

                        System.out.print("Disable this user? (yes/no): ");
                        String confirm = scanner.nextLine().trim().toLowerCase();
                        System.out.println(confirm); // Echo back user input to file/log
                        if (confirm.equals("yes")) {
                            disableUser(keycloak, realm, user.getId(), user.getUsername());
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
        System.out.println("Processed " + allProcessedUserIds.size() + " unique user(s).");
        System.out.println("\nDone.");
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

        String username = Optional.ofNullable(user.getUsername()).orElse("").toLowerCase();
        String email = Optional.ofNullable(user.getEmail()).orElse("").toLowerCase();
        String firstName = Optional.ofNullable(user.getFirstName()).orElse("").toLowerCase();
        String lastName = Optional.ofNullable(user.getLastName()).orElse("").toLowerCase();

        String fullName = (firstName + " " + lastName).trim();

        return username.contains(lowerTerm)
                || email.contains(lowerTerm)
                || firstName.contains(lowerTerm)
                || lastName.contains(lowerTerm)
                || fullName.contains(lowerTerm);
    }

    static void disableUser(Keycloak keycloak, String realm, String userId, String username) {
        try {
            UserResource userRes = keycloak.realm(realm).users().get(userId);
            UserRepresentation user = userRes.toRepresentation();
            if (!user.isEnabled()) {
                System.out.println("User is already disabled.");
                return;
            }

            if (SIMULATION_MODE) {
                System.out.printf("[SIMULATION] Would disable user %s (%s) in realm %s%n", username, userId, realm);
            } else {
                user.setEnabled(false);
                userRes.update(user);
                System.out.printf("[REAL] Disabled user %s (%s) in realm %s%n", username, userId, realm);
            }

        } catch (Exception e) {
            System.err.printf("Failed to disable user %s in realm %s: %s%n", userId, realm, e.getMessage());
        }
    }

    static Keycloak authenticateWithKeycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(KEYCLOAK_URL)
                .realm("master") // Usually "master" for client_credentials
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .grantType("client_credentials")
                .build();
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
                    originalOut.write(b);  // ✅ Use the original System.out
                    fileOut.write(b);
                }

                @Override
                public void flush() throws IOException {
                    originalOut.flush();
                    fileOut.flush();
                }
            }, true);  // Auto-flush enabled

            System.setOut(dualOut);
            System.setErr(dualOut);
            System.out.println("Logging to " + logFileName);
        } catch (IOException e) {
            System.err.println("Failed to set up log file: " + e.getMessage());
        }
    }

    static final String RESET = "\u001B[0m";
    static final String RED = "\u001B[31m";
    static final String GREEN = "\u001B[32m";
    static final String CYAN = "\u001B[36m";

    static void printEnvironmentBanner(String url) {
        String urlLower = url.toLowerCase();
        boolean isProd = !(urlLower.contains("dev") || urlLower.contains("test"));

        String banner = "\n==============================\n" +
                "  ENVIRONMENT: " + CYAN + url.toUpperCase() + RESET + "\n" +
                (isProd
                        ? RED + "  ⚠️PRODUCTION ENVIRONMENT — BE CAREFUL! ⚠️" + RESET + "\n"
                        : GREEN + "  (Non-prod environment)" + RESET + "\n") +
                "==============================";

        if (isProd) {
            Toolkit.getDefaultToolkit().beep();
        }

        System.out.println(banner);
    }

    static boolean isProdEnvironment(String url) {
        String lower = url.toLowerCase();
        return !(lower.contains("dev") || lower.contains("test"));
    }

}
