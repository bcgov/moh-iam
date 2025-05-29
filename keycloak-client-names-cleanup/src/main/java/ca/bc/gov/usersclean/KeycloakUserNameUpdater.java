package ca.bc.gov.usersclean;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

public class KeycloakUserNameUpdater {
	
    private static final String KEYCLOAK_URL = "KEYCLOAK_URL";
    private static final String CLIENT_ID = "CLIENT_ID";
    private static final String CLIENT_SECRET = "CLIENT_SECRET";
    public static final String INPUT_FILE_PATH = "INPUT_FILE_PATH";
    private static final String REALM = "REALM";
	
    // Simulation mode flag
    private static String SIMULATION_MODE = "SIMULATION_MODE";  // Default to simulation mode
    private static String PARENTHESES_OPEN = "(";
    private static boolean SIMULATION = true;
    private static String REGEX_PARENTHESES = "\\((.*)\\)";
    private static String REGEX_PARENTHESES_REMOVE = "\\([^()]*\\)";
    
    private static Properties prop = new Properties();
    
	public static void main(String[] args) throws IOException {
		loadProperties();
		SIMULATION = Boolean.parseBoolean(prop.getProperty(SIMULATION_MODE));
		updateUsers();
		
	}
    
    private static Keycloak authenticateWithKeycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(prop.getProperty(KEYCLOAK_URL))
                .realm(prop.getProperty(REALM))
                .clientId(System.getenv(prop.getProperty(CLIENT_ID)))
                .clientSecret(System.getenv(prop.getProperty(CLIENT_SECRET)))
                .grantType("client_credentials")
                .build();
    }    
    
    private static void updateUsers() {
    	
    	System.out.println("-----------Begin UpdateUsers-----------");
    	
    	Map<String, String> usernameMappings = loadUsernameMappings(prop.getProperty(INPUT_FILE_PATH));
    	
    	  // Authenticate with Keycloak
        Keycloak keycloak = authenticateWithKeycloak();
        
        // Use thread-safe collections for parallel processing
        ConcurrentLinkedQueue<String> successUpdates = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<String> noChanges = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<String> failedUpdates = new ConcurrentLinkedQueue<>();
        
        
        usernameMappings.entrySet().parallelStream().forEach(entry -> {
            String id = entry.getKey();
            String realm = entry.getValue();
            UpdateResult result = updateUsernameInKeycloak(keycloak, id, realm);

            switch (result) {
                case SUCCESS:
                    successUpdates.add(String.format("Updated userID [%s] -> Realm_id [%s]", id, realm));
                    break;
                case NOCHANGES:
                    noChanges.add(String.format("No changes have been made for userID [%s] Realm_id [%s]", id, realm));
                    break;
                case ERROR:
                    failedUpdates.add(String.format("Failed to update userID [%s] -> Realm_id [%s] due to an error. Check logs for details.", id, realm));
            }
        });
        
        // Output results
        System.out.println("Update Results (" + (SIMULATION ? "SIMULATION" : "REAL") + "):");
        successUpdates.forEach(System.out::println);
        noChanges.forEach(System.out::println);
        System.out.println("\nFailed Updates:");
        failedUpdates.forEach(System.out::println);

        System.out.println("Total Successful Updates: " + successUpdates.size());
        System.out.println("Total Unchanged users: " + noChanges.size());
        System.out.println("Total Failed Updates: " + failedUpdates.size());
        
        System.out.println("-----------End UpdateUsers-----------");

    }
    
    private static Map<String, String> loadUsernameMappings(String filePath) {
        Map<String, String> mappings = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue; 
                }
                String[] parts = line.split("\\|");
                if (parts.length == 2) {
                    mappings.put(parts[0], parts[1]); // userID : realm
                }
            }
        } catch (IOException e) {
			System.out.println("Exception during file reading " + e.getMessage());
			e.printStackTrace();
			return null;
		}
        return mappings;
    }

    
	private static UpdateResult updateUsernameInKeycloak(Keycloak keycloak, String id, String realm) {
		try {

			boolean flagFirstName = false;
			boolean flagLastName = false;
			UpdateResult result= UpdateResult.NOCHANGES;

			UserRepresentation userRepresentation = keycloak.realm(realm).users().get(id).toRepresentation();

			String firstName = userRepresentation.getFirstName();
			String lastName = userRepresentation.getLastName();

			if (containsParentheses(firstName)) {
				flagFirstName = true;
			}
			if (containsParentheses(lastName)) {
				flagLastName = true;
			}

			if (SIMULATION) {
				if (flagFirstName) {
					System.out.printf("[SIMULATION] Would update userID: [%s] firstName: [%s] with firstName: [%s] %n", id, firstName, removeParenthesesFromUserName(firstName));
					result = UpdateResult.SUCCESS; 
				}
				if (flagLastName) {
					System.out.printf("[SIMULATION] Would update userID: [%s] lastName: [%s] with lastName: [%s] %n", id, lastName, removeParenthesesFromUserName(lastName));
					result = UpdateResult.SUCCESS; 
				}

			} else {

				if (flagFirstName) {
					System.out.printf("[REAL] Updating update userID: [%s] firstName: [%s] with firstName: [%s] %n", id, firstName, removeParenthesesFromUserName(firstName));
					userRepresentation.setFirstName(removeParenthesesFromUserName(firstName));
					result = UpdateResult.SUCCESS; 
				}
				if (flagLastName) {
					System.out.printf("[REAL] Updating update userID: [%s] lastName: [%s] with lastName: [%s] %n", id, lastName, removeParenthesesFromUserName(lastName));
					userRepresentation.setLastName(removeParenthesesFromUserName(lastName));
					result = UpdateResult.SUCCESS; 
				}

				keycloak.realm(realm).users().get(id).update(userRepresentation);
			}

			return result;

		} catch (Exception e) {
			System.err.println("Error updating userId : " + id + ": " + e.getMessage());
			return UpdateResult.ERROR; // General error occurred
		}
	}

	
	private static boolean containsParentheses(String name) {
		Pattern pattern = Pattern.compile(REGEX_PARENTHESES);
		Matcher matcher = pattern.matcher(name);
		return matcher.find();
	}
	
	public static String removeParenthesesFromUserName(String name) {
		return name.replaceAll(REGEX_PARENTHESES_REMOVE, "").trim();
	}

	private static void loadProperties() {

		try (InputStream input = KeycloakUserNameUpdater.class.getClassLoader().getResourceAsStream("config.properties")) {

			if (input == null) {
				System.out.println("config.properties not found");
				return;
			}

			prop.load(input);

		} catch (IOException e) {
			System.out.println("Exception during loading properties file" + e.getMessage());
			return;
		}

	}
	
    // Enum to represent the result of an update attempt
    enum UpdateResult {
        SUCCESS,
        NOCHANGES,
        ERROR
    }
    
	
}
