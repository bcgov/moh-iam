import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class KeycloakUsernameChecker {

    // Keycloak configuration
//    private static final String KEYCLOAK_URL = "https://common-logon-test.hlth.gov.bc.ca/auth";
//    private static final String REALM = "moh_applications";
//    private static final String CLIENT_ID = "DAVID_SHARPE_DELETE_TEST_20250120.1634";
//    private static final String CLIENT_SECRET = System.getenv("DAVID_SHARPE_DELETE_TEST_20250120.1634");

    private static final String KEYCLOAK_URL = "https://common-logon-dev.hlth.gov.bc.ca/auth";
    private static final String REALM = "moh_applications";
    private static final String CLIENT_ID = "DAVID_SHARPE_DELETE_20250120.0924";
    private static final String CLIENT_SECRET = System.getenv("DAVID_SHARPE_DELETE_20250120.0924");

    public static final String INPUT_FILE_PATH = "C:\\Users\\david.a.sharpe\\Documents\\corrected_usernames.txt";

    public static void main(String[] args) throws IOException {

        try {
            // Load usernames from the file
            List<String> usernames = loadUsernamesFromFile(INPUT_FILE_PATH);

            // Authenticate with Keycloak
            Keycloak keycloak = authenticateWithKeycloak();

            // Process usernames using parallel streams
            List<UserResult> results = usernames.parallelStream()
                    .map(fileUsername -> fetchUserDetails(keycloak, fileUsername))
                    .filter(Objects::nonNull) // Remove any null results (e.g., if the user wasn't found)
                    .collect(Collectors.toList());

            // Output results
            System.out.println("Results for usernames in old format found in Keycloak:");
            results.forEach(result -> {
                System.out.println(String.format(
                        "File Username: %s, Keycloak Username: %s, phsa_windowsaccountname: %s",
                        result.fileUsername,
                        result.actualUsername != null ? result.actualUsername : "Not found",
                        result.phsaWindowsAccountName != null ? result.phsaWindowsAccountName : "Not found"
                ));
            });

            System.out.println("Count: " + results.size());

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            throw e;
        }
    }

    private static List<String> loadUsernamesFromFile(String filePath) throws IOException {
        List<String> usernames = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("samaccountname")) {
                    continue; // Skip headers and blank lines
                }
                String[] parts = line.split("\\s+");
                if (parts.length > 0) {
                    usernames.add(parts[0]); // Extract samaccountname
                }
            }
        }
        return usernames;
    }

    private static Keycloak authenticateWithKeycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(KEYCLOAK_URL)
                .realm(REALM)
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .grantType("client_credentials") // Use client credentials grant type
                .build();
    }

    private static UserResult fetchUserDetails(Keycloak keycloak, String fileUsername) {
        try {
            // Search for all users matching the old username
            List<UserRepresentation> users = keycloak.realm(REALM).users().searchByUsername(fileUsername, false);

            // Filter results to find the best match
            Optional<UserRepresentation> matchedUser = users.stream()
                    .filter(user -> user.getUsername().contains("\\") // Ensure old format with backslash
                            && user.getUsername().toLowerCase().endsWith("\\" + fileUsername.toLowerCase())) // Match username after the backslash
                    .filter(user -> {
                        // Fetch detailed user info to validate federated identities
                        String userId = user.getId();
                        UserRepresentation detailedUser = keycloak.realm(REALM).users().get(userId).toRepresentation();
                        List<FederatedIdentityRepresentation> federatedIdentities = detailedUser.getFederatedIdentities();
                        if (federatedIdentities == null) {
                            System.out.println("[INFO] User has no federated identities: " + fileUsername);
                        }
                        return federatedIdentities == null || federatedIdentities.stream()
                                .anyMatch(f -> f.getIdentityProvider().equals("phsa"));
                    })
                    .findFirst();

            if (matchedUser.isEmpty()) {
                return null; // No valid user found
            }

            // Get the matched user
            UserRepresentation user = matchedUser.get();
            String actualUsername = user.getUsername();

            // Retrieve the phsa_windowsaccountname attribute if present
            Map<String, List<String>> attributes = user.getAttributes();
            String phsaWindowsAccountName = null;
            if (attributes != null && attributes.containsKey("phsa_windowsaccountname")) {
                phsaWindowsAccountName = attributes.get("phsa_windowsaccountname").get(0);
            }

            return new UserResult(fileUsername, actualUsername, phsaWindowsAccountName);

        } catch (Exception e) {
            System.err.println("Error fetching details for username: " + fileUsername + " - " + e.getMessage());
            e.printStackTrace();
            return null; // Return null if an error occurs
        }
    }

    // Inner class to store results
    private static class UserResult {
        String fileUsername;
        String actualUsername;
        String phsaWindowsAccountName;

        UserResult(String fileUsername, String actualUsername, String phsaWindowsAccountName) {
            this.fileUsername = fileUsername;
            this.actualUsername = actualUsername;
            this.phsaWindowsAccountName = phsaWindowsAccountName;
        }
    }
}
