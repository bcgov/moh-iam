import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import model.csv.UserData;
import service.CSVFileService;
import service.KeycloakService;

/**
 * Program to handle updating Keycloak user info in a bulk upload. The program accepts a csv file with fields:
 * 	User - Roles
 * 	e.g. 
 * 	testuser1,"role_1, role_2"
 * 	testuser2,"role_1, role_3"
 * 
 * Currently this program will:
 * 	- Add users if they don't exist
 * 	- Add the specified roles to the user. No roles are removed
 *
 * The program does not remove any user configuration and the same file can be imported multiple times to produce the same end result.
 * 
 */
public class Main {

    private static final String DEFAULT_ENV = "dev";

	public static void main(String[] args) throws Exception {
    	System.out.println("Begin loading Keycloak user data...");
    	
    	String environment = determineEnvironment(args);
        
        Properties configProperties = getProperties(environment);
    	KeycloakService keycloakService = new KeycloakService(configProperties);    	   
	    CSVFileService csvFileService = new CSVFileService();
	    
		try {
			List<UserData> userDataList = csvFileService.extractFileInfo(configProperties.getProperty("data-file-location"));
			
		    keycloakService.updateKeycloakData(configProperties.getProperty("application"), userDataList);

	    	System.out.println("Completed loading Keycloak user data.");
		} catch (Exception e) {
			System.out.println("Loading Keycloak user data could not be completed due to: " + e.getMessage());
		}
		
    }

	private static String determineEnvironment(String[] args) {
		String environment = DEFAULT_ENV;
        if (args != null && args.length != 0) {
        	environment = args[0]; 
        	System.out.println("Running against environment: " + environment);
        }
		return environment;
	}

    private static Properties getProperties(String environment) throws Exception {
        URL defaultLocation = Main.class.getClassLoader().getResource("configuration-" + environment + ".properties");
        String configPath = new File(defaultLocation.toURI()).getAbsolutePath();

        Properties configProperties = new Properties();
        File file = new File(configPath);

        InputStream inputStream = (file.exists())? new FileInputStream(file) : Main.class.getResourceAsStream(configPath);
        Objects.requireNonNull(inputStream, String.format("Configuration file not found at '%s'.", configPath));
        configProperties.load(inputStream);
        return configProperties;
    }
}