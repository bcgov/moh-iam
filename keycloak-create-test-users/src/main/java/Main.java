import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.*;

import java.io.*;
import java.net.URL;

import java.util.*;
import java.util.logging.Logger;

//The view's job is to decide
// what the user will see on their screen, and how.

public class Main {
    private static String configPath;
    private static UserService userService;
    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    /**
     * Application entry point
     * Reads configuration parameters, loads user list, creates users in KC
     * @param args String[]
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        if (args != null && args.length != 0) {
            configPath = args[0];
        } else {
            URL defaultLocation = Main.class.getClassLoader().getResource("configuration.properties");
            configPath = new File(defaultLocation.toURI()).getAbsolutePath();
        }
        LOG.info(String.format("Configuration file expected at '%s'.", configPath));

        // initializes the userService
        userService = new UserService(configPath);
        
        //Get the list of users to add
        UserList userList = userService.parseUsersFromJSON();
        
        //Clear out users if they already exist
        userService.deleteUsersFromKeycloak(userList);
        
        //Create the users
        userService.createUsersInKeycloak(userList);
        
        //Add roles
        userService.addAllClientRolesInKeyCloak(userList);
    }

    /**
     * prints all the usernames of the users in that realm
     * @param usersResource
     */
    private static void printKeycloakUserList(UsersResource usersResource) {
        List<UserRepresentation> users = usersResource.list();
        System.out.println("list of all users:");

        for (UserRepresentation user : users) {
            String name = user.getUsername();
            String id = user.getId();
            System.out.println("\t" + name + " id: " + id);
        }
    }

}
 