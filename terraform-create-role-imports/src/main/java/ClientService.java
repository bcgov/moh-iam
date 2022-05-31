import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.ClientRepresentation;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;



public class ClientService {
    private String inputFile;
    private String outputFile;
    private String resourcePath;
    private Keycloak keycloak;
    private RealmResource realmResource;
    private UsersResource usersResource;

    private Client client;

    /**
     * constructor for UserService
     * @param configPath the path to the configuration file
     */
    public ClientService(String configPath){
        try {
            setConfigurations(configPath);
        }catch (IOException e){
            e.printStackTrace();
        }

        setClient();
    }

    /**
     * writes the import statements to an output file
     */
    public void ImportAllRoles() {
        try {
            FileWriter file = new FileWriter(outputFile);
            String clientID = client.getClientID();
            RolesResource rolesResource = realmResource.clients().get(clientID).roles();
            // doing this so that editing the input file will change which roles imports will be generated
            for(String role: client.getRoles()){
                String roleID = rolesResource.get(role).toRepresentation().getId();
                // TODO: to change the output, edit this line:
                String importStatement = "terraform import " + resourcePath + ".keycloak_role.ROLES[\\\"" + role + "\\\"]" + realmResource.toRepresentation().getId() + "/" + roleID + "\n";
                file.write(importStatement);
            }
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * sets up the inputFile path and keycloak. Also sets up the RealmResource
     * and its corresponding usersResource.
     * @param configPath
     * @throws IOException
     */
    public void setConfigurations(String configPath) throws IOException {
        Properties configProperties = new Properties();
        File file = new File(configPath);

        InputStream inputStream;
        if (file.exists()) {
            inputStream = new FileInputStream(file);
        } else {
            inputStream = Main.class.getResourceAsStream(configPath);
        }
        Objects.requireNonNull(inputStream, String.format("Configuration file not found at '%s'.", configPath));
        configProperties.load(inputStream);

        inputFile = configProperties.getProperty("inputFile");
        checkMandatory(inputFile);
        outputFile = configProperties.getProperty("outputFile");
        checkMandatory(outputFile);
        resourcePath = configProperties.getProperty("resourcePath");
        checkMandatory(resourcePath);
        String serverURL = configProperties.getProperty("serverURL");
        checkMandatory(serverURL);
        String realm = configProperties.getProperty("realm");
        checkMandatory(realm);
        String clientID = configProperties.getProperty("clientID");
        checkMandatory(clientID);
        String clientSecret = configProperties.getProperty("clientSecret");
        checkMandatory(clientSecret);

        keycloak =  KeycloakBuilder.builder() //
                .serverUrl(serverURL) //
                .realm(realm) //
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS) //
                .clientId(clientID) //
                .clientSecret(clientSecret) //
                .build();

        realmResource = keycloak.realm(realm);
        usersResource = realmResource.users();
    }

    /**
     * checks whether or not String is blank, and throws
     * @param value
     */
    private static void checkMandatory(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(String.format("Value is mandatory but was '%s'.", value));
        }
    }

    // JSON methods
    /**
     * creates a new client based on input file
//     */
    private void setClient(){
        try {
            Reader reader = Files.newBufferedReader(Paths.get(inputFile));
            JsonObject parser = (JsonObject) Jsoner.deserialize(reader);

            String clientID = (String) parser.get("clientId");
            String clientUUID = getClientUUID(clientID);

            JsonArray jsonarray = (JsonArray) parser.get("roles");
            ArrayList<String> roles = new ArrayList();
            jsonarray.forEach(entry -> {
                try {
                    String role = ((JsonObject) entry).get("name").toString();
                    roles.add(role);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            client = new Client(clientID,clientUUID,roles);

            reader.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * gets the Client's UUID from a clientID
     *
     * @param clientID
     * @return String representation of the clientUUID
     * @throws Exception when multiple clients or no clients found
     */
    public String getClientUUID(String clientID) throws Exception {
        ClientsResource clientsResource = realmResource.clients();
        List<ClientRepresentation> clientRepresentationList = clientsResource.findByClientId(clientID);
        if (clientRepresentationList.size() > 1) {
            throw new Exception("more than 1 client found");
        } else if (clientRepresentationList.size() < 1) {
            throw new Exception("client not found");
        } else {
            String clientUUID = clientRepresentationList.get(0).getId();
            return clientUUID;
        }
    }

}

