package ca.bc.gov.hlth;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class KeycloakUsernameUpdater {

    // Keycloak configuration
    private static final String KEYCLOAK_URL = "https://common-logon-test.hlth.gov.bc.ca/auth";
    private static final String REALM = "moh_citizen";
    private static final String CLIENT_ID = "BCMOHAD-31351-service-account";
    private static final String CLIENT_SECRET = System.getenv("BCMOHAD-31351-service-account");

//    private static final String KEYCLOAK_URL = "https://common-logon-test.hlth.gov.bc.ca/auth";
//    private static final String REALM = "moh_applications";
//    private static final String CLIENT_ID = "DAVID_SHARPE_DELETE_TEST_20250120.1634";
//    private static final String CLIENT_SECRET = System.getenv("DAVID_SHARPE_DELETE_TEST_20250120.1634");

//    private static final String KEYCLOAK_URL = "https://common-logon-dev.hlth.gov.bc.ca/auth";
//    private static final String REALM = "moh_applications";
//    private static final String CLIENT_ID = "DAVID_SHARPE_DELETE_20250120.0924";
//    private static final String CLIENT_SECRET = System.getenv("DAVID_SHARPE_DELETE_20250120.0924");

    public static final String INPUT_FILE_PATH = "C:\\Users\\david.a.sharpe\\Desktop\\RFC-20260424-04-BCMOHAD-31395-PROD-KEYCLOAK_update_PHSA_usernames_moh_citizen\\output-RFC-20260424-04-BCMOHAD-31395-PROD-KEYCLOAK_update_PHSA_usernames_moh_citizen_withInfoSys.txt";

    // Simulation mode flag
    private static boolean SIMULATION_MODE = true;  // Default to simulation mode

    public static void main(String[] args) throws IOException {
        // Check if the mode is passed as an argument
        if (args.length > 0 && args[0].equalsIgnoreCase("real")) {
            SIMULATION_MODE = false;
        }

        System.out.println("Running in " + (SIMULATION_MODE ? "SIMULATION" : "REAL") + " mode.");

        // Load username mappings from the file
        Map<String, String> usernameMappings = loadUsernameMappings(INPUT_FILE_PATH);

        // Authenticate with Keycloak
        Keycloak keycloak = authenticateWithKeycloak();

        // Use thread-safe collections for parallel processing
        ConcurrentLinkedQueue<String> successUpdates = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<String> failedUpdates = new ConcurrentLinkedQueue<>();

        // Process username updates in parallel
        usernameMappings.entrySet().parallelStream().forEach(entry -> {
            String oldUsername = entry.getKey();
            String newUsername = entry.getValue() + "@phsa";
            UpdateResult result = updateUsernameInKeycloak(keycloak, oldUsername, newUsername);

            switch (result) {
                case SUCCESS:
                    successUpdates.add(String.format("Updated %s -> %s", oldUsername, newUsername));
                    break;
                case NOT_FOUND:
                    // Suppress output for not found users
                    break;
                case ERROR:
                    failedUpdates.add(String.format("Failed to update %s -> %s due to an error. Check logs for details.", oldUsername, newUsername));
                    break;
            }
        });

        // Output results
        System.out.println("Update Results (" + (SIMULATION_MODE ? "SIMULATION" : "REAL") + "):");
        successUpdates.forEach(System.out::println);
        System.out.println("\nFailed Updates:");
        failedUpdates.forEach(System.out::println);

        System.out.println("Total Successful Updates: " + successUpdates.size());
        System.out.println("Total Failed Updates: " + failedUpdates.size());
    }

    private static Map<String, String> loadUsernameMappings(String filePath) throws IOException {
        Map<String, String> mappings = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split(",", -1);
                if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                    throw new IOException("Invalid mapping at line " + lineNumber + ": expected domain\\username,email");
                }

                mappings.put(parts[0].trim(), parts[1].trim()); // Map old username to new username
            }
        }
        return mappings;
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

    private static UpdateResult updateUsernameInKeycloak(Keycloak keycloak, String oldUsername, String newUsername) {
        try {
            // Search for all users matching the old username
            List<UserRepresentation> users = keycloak.realm(REALM).users().searchByUsername(oldUsername, false);

            // Filter results to find a valid match
            Optional<UserRepresentation> matchedUser = users.stream()
                    .filter(user -> matchesOldUsername(user.getUsername(), oldUsername))
                    .filter(user -> {
                        // Fetch detailed user info to validate federated identities
                        String userId = user.getId();
                        UserRepresentation detailedUser = keycloak.realm(REALM).users().get(userId).toRepresentation();
                        List<FederatedIdentityRepresentation> federatedIdentities = detailedUser.getFederatedIdentities();
                        if (federatedIdentities == null) {
                            System.out.println("[INFO] User has no federated identities: " + user.getUsername());
                        }
                        return federatedIdentities == null || federatedIdentities.stream()
                                .anyMatch(f -> f.getIdentityProvider().equals("phsa"));
                    })
                    .findFirst();

            if (matchedUser.isEmpty()) {
                return UpdateResult.NOT_FOUND; // No valid user found
            }

            // Update username for the matched user
            UserRepresentation userToUpdate = matchedUser.get();
            if (SIMULATION_MODE) {
                System.out.printf("[SIMULATION] Would update username for %s to %s (ID: %s)%n", userToUpdate.getUsername(), newUsername, userToUpdate.getId());
            } else {
                System.out.printf("[REAL] Updating username for %s to %s (ID: %s)%n", userToUpdate.getUsername(), newUsername, userToUpdate.getId());
                userToUpdate.setUsername(newUsername);
                keycloak.realm(REALM).users().get(userToUpdate.getId()).update(userToUpdate);
            }

            return UpdateResult.SUCCESS;

        } catch (Exception e) {
            System.err.println("Error updating username for " + oldUsername + ": " + e.getMessage());
            e.printStackTrace();
            return UpdateResult.ERROR; // General error occurred
        }
    }

    private static boolean matchesOldUsername(String keycloakUsername, String oldUsername) {
        if (keycloakUsername == null) {
            return false;
        }

        String normalizedKeycloakUsername = keycloakUsername.toLowerCase();
        String normalizedOldUsername = oldUsername.toLowerCase();

        if (normalizedOldUsername.contains("\\")) {
            return normalizedKeycloakUsername.equals(normalizedOldUsername);
        }

        return normalizedKeycloakUsername.contains("\\")
                && normalizedKeycloakUsername.endsWith("\\" + normalizedOldUsername);
    }

    // Enum to represent the result of an update attempt
    enum UpdateResult {
        SUCCESS,
        NOT_FOUND,
        ERROR
    }
}
