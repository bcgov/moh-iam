
import java.io.*;

import java.net.URL;
import java.util.logging.Logger;

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

        clientService.createResources();
        clientService.createImports();

        clientService.closeFile();
    }
}



