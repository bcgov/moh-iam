import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.*;

import java.io.*;

import java.util.*;
import java.util.logging.Logger;

//The view's job is to decide
// what the user will see on their screen, and how.

public class Main {
    private static String configPath;
    private static UserService userService;
    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {
        if (args != null && args.length != 0) {
            configPath = args[0];
        } else {
            configPath = "configuration.properties";
        }
        LOG.info(String.format("Configuration file expected at '%s'.", configPath));

        userService = new UserService(configPath);

        userService.addAllToKeyCloak();
        userService.addAllClientRolesInKeyCloak();
//        userService.deleteAllFromKeyCloak();
    }

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
 