
import java.io.*;

import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

//The view's job is to decide
// what the user will see on their screen, and how.

public class Main {
    private static String configPath;
    private static ClientService clientService;
    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {
        if (args != null && args.length != 0) {
            configPath = args[0];
        } else {
            URL defaultLocation = Main.class.getClassLoader().getResource("configuration.properties");
            configPath = new File(defaultLocation.toURI()).getAbsolutePath();
        }
        LOG.info(String.format("Configuration file expected at '%s'.", configPath));

        clientService = new ClientService(configPath);

        clientService.ImportAllRoles();



    }



    private static void writeToFile() {
        String filePath = "C:\\Users\\valery.angelique\\Downloads\\create-healthnet-test-users-master\\create-healthnet-test-users-master\\src\\main\\java\\data\\output.txt";
        ArrayList<String> roles = new ArrayList<>();
        roles.add("ajdnasipf");
        try {
            FileWriter file = new FileWriter(filePath);
            for(String role: roles) file.write(role);
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
 