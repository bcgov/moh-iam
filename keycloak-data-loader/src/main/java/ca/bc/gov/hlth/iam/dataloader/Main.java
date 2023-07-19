
package ca.bc.gov.hlth.iam.dataloader;
 
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.hlth.iam.dataloader.model.csv.UserData;
import ca.bc.gov.hlth.iam.dataloader.service.CSVFileService;
import ca.bc.gov.hlth.iam.dataloader.service.EnvironmentEnum;
import ca.bc.gov.hlth.iam.dataloader.service.KeycloakService;

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

	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	private static final EnvironmentEnum DEFAULT_ENV = EnvironmentEnum.DEV;

	private static final String CONFIG_FILE_NAME_TEMPLATE = "configuration-%s.properties";
	
    private static final String CONFIG_PROPERTY_DATA_FILE_LOCATION = "data-file-location";

	private static final String CONFIG_PROPERTY_APPLICATION = "application";

    public static void main(String[] args) throws Exception {
    	logger.info("Begin loading Keycloak user data with args: {}", Arrays.toString(args));
    	
    	processDataLoad(args);
    }

	private static void processDataLoad(String[] args) throws Exception {
    	EnvironmentEnum environment = determineEnvironment(args);
		Properties configProperties = getProperties(environment);
    	KeycloakService keycloakService = new KeycloakService(configProperties, environment);    	   
	    CSVFileService csvFileService = new CSVFileService();
	    
		List<UserData> userDataList = csvFileService.extractFileInfo(configProperties.getProperty(CONFIG_PROPERTY_DATA_FILE_LOCATION));

		try {			
		    keycloakService.updateKeycloakData(configProperties.getProperty(CONFIG_PROPERTY_APPLICATION), userDataList);

	    	logger.info("Completed loading Keycloak user data.");
		} catch (Exception e) {
			logger.error("Loading Keycloak user data could not be completed due to: " + e.getMessage());
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