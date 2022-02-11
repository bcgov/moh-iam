
import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

public class UserService {

    private String inputFile;
    private Keycloak keycloak;
    private String roleRealmName;
    private String passwordRealmName;
    private RealmResource realmResource;
    private UsersResource usersResource;

    /**
     * constructor for UserService
     *
     * @param configPath the path to the configuration file
     */
    public UserService(String configPath) {
        try {
            setConfigurations(configPath);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * sets up the inputFile path and keycloak. Also sets up the RealmResource
     * and its corresponding usersResource.
     *
     * @param configPath
     * @throws IOException
     */
    public void setConfigurations(String configPath) throws IOException, URISyntaxException {
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

        URL defaultLocation = Main.class.getClassLoader().getResource(configProperties.getProperty("inputFile"));
        inputFile = new File(defaultLocation.toURI()).getAbsolutePath();
        checkMandatory(inputFile);
        String serverURL = configProperties.getProperty("serverURL");
        checkMandatory(serverURL);
        passwordRealmName = configProperties.getProperty("passwordRealm");
        checkMandatory(passwordRealmName);
        roleRealmName = configProperties.getProperty("roleRealm");
        checkMandatory(roleRealmName);
        String clientID = configProperties.getProperty("clientID");
        checkMandatory(clientID);
        String clientSecret = configProperties.getProperty("clientSecret");
        checkMandatory(clientSecret);

        keycloak = KeycloakBuilder.builder() //
                .serverUrl(serverURL) //
                .realm("master") //
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS) //
                .clientId(clientID) //
                .clientSecret(clientSecret) //
                .build();

        realmResource = keycloak.realm(roleRealmName);
        usersResource = realmResource.users();
    }

    /**
     * checks whether or not String is blank, and throws
     *
     * @param value
     */
    private static void checkMandatory(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(String.format("Value is mandatory but was '%s'.", value));
        }
    }

    // getters/setters

    /**
     * sets the realmResource and its corresponding usersResource
     *
     * @param realmName
     */
    public void setRealmResource(String realmName) {
        realmResource = keycloak.realm(realmName);
        usersResource = realmResource.users();
    }


    // JSON methods

    /**
     * Reads a list of User objects from the configured JSON file into a
     * UserList
     *
     * @return UserList
     */
    public UserList parseUsersFromJSON() {
        UserList userList = new UserList();
        try {
            InputStream inputStream;
            File file = new File(inputFile);
            if (file.exists()) {
                inputStream = new FileInputStream(file);
            } else {
                inputStream = this.getClass().getResourceAsStream(inputFile);
            }
            Reader reader = new BufferedReader(new InputStreamReader(inputStream));
            JsonArray parser = (JsonArray) Jsoner.deserialize(reader);

            parser.forEach(entry -> {
                JsonObject jsonUser = (JsonObject) entry;
                try {
                    User user = getUserFromJSON(jsonUser);

                    if (userList.contains(user.getUsername())) {
                        throw new Exception("duplicate username found: " + user.getUsername());
                    }
                    userList.addUser(user);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            reader.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return userList;
    }

    /**
     * Makes a User object out of JSON data
     *
     * @param jsonUser JsonObject
     * @return User
     * @throws Exception when no username is found
     */
    private User getUserFromJSON(JsonObject jsonUser) throws Exception {
        String username = (String) jsonUser.get("username");
        String password = (String) jsonUser.get("password");
        String firstName = (String) jsonUser.get("firstname");
        String lastName = (String) jsonUser.get("lastname");
        String email = (String) jsonUser.get("email");

        if (username == null) {
            throw new Exception("no username");
        }

        User user = (password != null) ? new User(username, password) : new User(username);
        if (firstName != null) {
            user.getUserRepresentation().setFirstName(firstName);
        }
        if (lastName != null) {
            user.getUserRepresentation().setLastName(lastName);
        }
        if (email != null) {
            user.getUserRepresentation().setEmail(email);
        }

        Map<String, JsonArray> applications = (Map<String, JsonArray>) jsonUser.get("applications");
        if (applications != null) {
            applications.forEach((client, roles) -> roles.forEach(role -> user.recordClientRoles(client, (String) role)));
        }

        Map<String, String> attributes = (Map<String, String>) jsonUser.get("attributes");
        if (attributes != null) {
            attributes.forEach((key, val) -> user.addAttribute(key, val));
        }
        return user;
    }

    // keycloak methods

    /**
     * adds all the Users in UserList to keycloak
     */
    public void createUsersInKeycloak(UserList userList) {
        for (User user : userList) {
            createUserInKeycloak(user.getUserRepresentation());
        }
    }

    /**
     * adds a single user to Keycloak.
     *
     * @param userRepresentation
     */
    public void createUserInKeycloak(UserRepresentation userRepresentation) {
        try {
            Response response = usersResource.create(userRepresentation);
            System.out.printf("Response: %s %s%n", response.getStatus(), response.getStatusInfo());
            System.out.println(response.getLocation());
            String userId = CreatedResponseUtil.getCreatedId(response);
            userRepresentation.setId(userId);
            System.out.printf(userRepresentation.getUsername() + " (" + realmResource.toRepresentation().getRealm() + ") created with userId: %s%n", userId);
        } catch (WebApplicationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the passwords of all users in UserList. temporarily changes realmresource to the password's realm,
     * creates users in that realm and sets the passwords in that realm
     *
     * @param userList
     */
    public void setUserPasswordsInKeycloak(UserList userList) {
        setRealmResource(passwordRealmName);
        for (User user : userList) {
            createUserInKeycloak(user.getUserRepresentation());
            setUserPasswordInKeycloak(user);
        }
        setRealmResource(roleRealmName);
    }

    /**
     * Sets the password of a single user in Keycloak.
     *
     * @param user
     */
    public void setUserPasswordInKeycloak(User user) {
        try {
            String userID = getUserID(user);
            UserResource userResource = usersResource.get(userID);

            CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
            credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
            credentialRepresentation.setValue(user.getPassword());

            userResource.resetPassword(credentialRepresentation);
            System.out.println(user.getUserRepresentation().getUsername() + "'s password has been reset.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * deletes all users in UserList from keycloak, from both the roleReamn and password realm
     */
    public void deleteUsersFromKeycloak(UserList userList) {
        for (User user : userList) {
            deleteUserFromKeycloak(user);
        }

        setRealmResource(passwordRealmName);
        for (User user : userList) {
            deleteUserFromKeycloak(user);
        }
        setRealmResource(roleRealmName);
    }

    /**
     * deletes specified used from Keycloak
     *
     * @param user
     */
    public void deleteUserFromKeycloak(User user) {
        try {
            String userID = getUserID(user);
            usersResource.delete(userID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * adds all the client roles of all the users in UserList to Keycloak
     */
    public void addAllClientRolesInKeyCloak(UserList userList) {
        for (User user : userList) {
            addClientRolesInKeyCloak(user);
        }
    }

    /**
     * adds all of the specified user's client roles to Keycloak
     *
     * @param user
     */
    public void addClientRolesInKeyCloak(User user) {
        try {//try looking for the user
            String userID = getUserID(user);
            for (String clientID : user.getClientIDs()) {
                try {
                    String clientUUID = getClientUUID(clientID);

                    ClientResource clientResource = realmResource.clients().get(clientUUID);
                    List<RoleRepresentation> rolesToAdd = getRolesToAdd(user.getClientRoles(clientID), clientResource);

                    UserResource userResource = realmResource.users().get(userID);
                    RoleScopeResource roleScopeResource = userResource.roles().clientLevel(clientUUID);

                    roleScopeResource.add(rolesToAdd);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * gets the userID of the user
     *
     * @param user
     * @return String representation of the UserID
     * @throws Exception if multiple usernames/no usernames found
     */
    public String getUserID(User user) throws Exception {
        String result = user.getUserRepresentation().getId();
        if (result != null) {
            return result;
        }

        List<UserRepresentation> users = usersResource.search(user.getUserRepresentation().getUsername());
        if (users.size() > 1) {
            throw new Exception("error: more than 1 user found");
        } else if (users.size() == 0) {
            throw new Exception("no users with that username found");
        } else {
            return users.get(0).getId();
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

    /**
     * converts a collection of role names to a collection of its
     * RoleRepresentations
     *
     * @param roles          a set of role names to add
     * @param clientResource Keycloak API's interface for the current client
     * @return a list of RoleRepresentations
     */
    public List<RoleRepresentation> getRolesToAdd(Set<String> roles, ClientResource clientResource) {
        List<RoleRepresentation> result = new ArrayList<>();
        RolesResource rolesResource = clientResource.roles();
        for (String roleName : roles) {
            try {
                List<RoleRepresentation> roleRepresentationList = rolesResource.list(roleName, true);
                if (roleRepresentationList.size() > 1) {
                    throw new Exception("multiple roles with name " + roleName + " found");
                }
                if (roleRepresentationList.size() < 1) {
                    throw new Exception(roleName + " not found");
                }
                result.addAll(roleRepresentationList);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
