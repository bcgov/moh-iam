
package ca.bc.gov.hlth.iam.clientgeneration;
 
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.hlth.iam.clientgeneration.service.EnvironmentEnum;
import ca.bc.gov.hlth.iam.clientgeneration.service.KeycloakService;

/**
 * Program to create new clients. The clients are created with a default set of properties currently suited to creating
 * clients for use by PPM API clients. The main characteristic of these clients is that they authenticate by "Signed JWT".
 *  
 * Currently this program will output a CVS file containing:
 * 	- A list of Client IDs for the newly created clients
 * 	- The client's associated cert info which includes:
 * 		- cert file name
 * 		- cert file alias
 * 		- key password
 * 		- store password
 * 		- cert expiry date
 * 	The certificates will also be genearated and saved locally. 
 *  
 */
public class Main {

	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	private static final EnvironmentEnum DEFAULT_ENV = EnvironmentEnum.DEV;

	private static final String CONFIG_FILE_NAME_TEMPLATE = "configuration-%s.properties";

    public static void main(String[] args) throws Exception {
    	logger.info("Begin processing clients with args: {}", Arrays.toString(args));
    	
    	processClients(args);
    }

	private static void processClients(String[] args) throws Exception {
    	EnvironmentEnum environment = determineEnvironment(args);
		Properties configProperties = getProperties(environment);
		
		KeycloakService keycloakService = new KeycloakService(configProperties, environment);    	   

		try {			
		    keycloakService.addClients(configProperties, determineNumberOfClients(args), determineClientStartNumber(args));

	    	logger.info("Completed creating clients.");
		} catch (Exception e) {
			logger.error("Creating clients could not be completed due to: " + e.getMessage());
			throw e; 
		}
	}

	private static EnvironmentEnum determineEnvironment(String[] args) {
		EnvironmentEnum environment = DEFAULT_ENV;
        if (args.length != 0) {
        	environment = EnvironmentEnum.valueOf(args[0].toUpperCase());
        	logger.info("Running against environment: {}", environment);
        }
		return environment;
	}

	private static int determineNumberOfClients(String[] args) {
		int numberOfClients = 1;
        if (args.length >= 2) {
        	numberOfClients = Integer.valueOf(args[1]);
        	logger.info("Number of clients to be created: {}", numberOfClients);
        }
		return numberOfClients;
	}

	private static int determineClientStartNumber(String[] args) {
		int clientStartNumber = 1;
        if (args.length >= 3) {
        	clientStartNumber = Integer.valueOf(args[2]);
        	logger.info("Client start number: {}", clientStartNumber);
        }
		return clientStartNumber;
	}

    private static Properties getProperties(EnvironmentEnum environmen) throws Exception {
        URL defaultLocation = Main.class.getClassLoader().getResource(String.format(CONFIG_FILE_NAME_TEMPLATE, environmen.getValue()));
        String configPath = new File(defaultLocation.toURI()).getAbsolutePath();
        File file = new File(configPath);

        InputStream inputStream = (file.exists())? new FileInputStream(file) : Main.class.getResourceAsStream(configPath);
        Objects.requireNonNull(inputStream, String.format("Configuration file not found at '%s'.", configPath));
        
        Properties configProperties = new Properties();
        configProperties.load(inputStream);
        return configProperties;
    }
}