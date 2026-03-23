package throwaway;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.UserRepresentation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;

/**
 * NOTE: This is a throwaway utility script intended for DEV/TEST cleanup only.
 * It is not production-hardened (minimal validation, logging, and safeguards).
 * Do not use in PROD without review and additional controls.
 */
public class KeycloakBulkUserDeleter {

    private static boolean SIMULATION_MODE = true;

    public static void main(String[] args) throws Exception {

        String inputFile = "C:\\Users\\david.a.sharpe\\Desktop\\delete.txt";
        String realm = "moh_applications";
        String envArg = "DEV";

        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--input")) inputFile = args[++i];
            if (args[i].equalsIgnoreCase("--realm")) realm = args[++i];
            if (args[i].equalsIgnoreCase("--env")) envArg = args[++i].toUpperCase();
            if (args[i].equalsIgnoreCase("--real")) SIMULATION_MODE = false;
        }

        if (inputFile == null || realm == null || envArg == null) {
            System.err.println("Usage: --input file.txt --realm REALM --env DEV|TEST|PROD [--real]");
            System.exit(1);
        }

        Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(getUrl(envArg))
                .realm("moh_applications")
                .clientId(getClientId(envArg))
                .clientSecret(System.getenv(getClientId(envArg)))
                .grantType("client_credentials")
                .build();

        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        String username;

        while ((username = reader.readLine()) != null) {

            username = username.trim();
            if (username.isEmpty()) continue;

            List<UserRepresentation> users = keycloak.realm(realm).users().search(username, true);

            String finalUsername = username;
            UserRepresentation user = users.stream()
                    .filter(u -> finalUsername.equalsIgnoreCase(u.getUsername()))
                    .findFirst()
                    .orElse(null);

            if (user == null) {
                System.out.println("User not found: " + username);
                continue;
            }

            if (SIMULATION_MODE) {
                System.out.println("[SIMULATION] Would delete: " + username + " (" + user.getId() + ")");
            } else {
                keycloak.realm(realm).users().delete(user.getId());
                System.out.println("[REAL] Deleted: " + username + " (" + user.getId() + ")");
            }
        }

        keycloak.close();
        System.out.println("Done.");
    }

    // reuse your env config logic if you want
    static String getUrl(String env) {
        return switch (env) {
            case "DEV" -> "https://common-logon-dev.hlth.gov.bc.ca/auth";
            case "TEST" -> "https://common-logon-test.hlth.gov.bc.ca/auth";
            case "PROD" -> throw new IllegalArgumentException("Not intended for PROD use. See class comment");
            default -> throw new RuntimeException("Invalid env");
        };
    }

    static String getClientId(String env) {
        return switch (env) {
            case "DEV" -> "immsbc-bulk-load-serviceaccount-dev";
            case "TEST" -> "immsbc-bulk-load-serviceaccount";
            case "PROD" -> throw new IllegalArgumentException("Not intended for PROD use. See class comment");
            default -> throw new RuntimeException("Invalid env");
        };
    }
}