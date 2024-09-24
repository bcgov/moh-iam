package ca.bc.gov.hlth.iam.clientgeneration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.hlth.iam.clientgeneration.service.EnvironmentEnum;
import ca.bc.gov.hlth.iam.clientgeneration.service.KeycloakService;

/**
 * Program to create new clients. The clients are created with a default set of
 * properties currently suited to creating clients for use by PPM API clients.
 * The main characteristic of these clients is that they authenticate by "Signed
 * JWT".
 * 
 * The program takes the following four arguments:
 * 	- The environment in which it should be run, valid vaues are 
 * 		- dev
 * 		- prod
 *	- The batch number e.g. 3 if this the third time it is being run in this environment
 *	- The number of clients to be created.
 *	- The seed number for the first client.
 * 
 * It can be run using the maven exec command with provided arguments e.g.
 *	 mvn compile exec:java -Dexec.args="dev 4 1 520"
 *
 * Currently this program will output a CVS file containing:
 *   - A list of Client IDs for the newly created clients
 *   - The client's associated cert info which includes:
 *	 - cert file name
 *	 - cert file alias
 *	 - key password
 *	 - store password
 *	 - cert expiry date
 * The certificates will also be genearated and saved locally.
 */
public class Main {

	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	private static final EnvironmentEnum DEFAULT_ENV = EnvironmentEnum.DEV;

	private static final String CONFIG_FILE_NAME_TEMPLATE = "configuration-%s.properties";

	private static final String PRODUCTION_ENVIRONMENT_WILL_BE_UPDATED_MSG = "Production environment will be updated";

	private static final int NUMBER_OF_CLIENTS_MAX = 50;

	private static final int NUMBER_OF_CLIENTS_WARN = 20;

	public static void main(String[] args) throws Exception {
		logger.info("Begin processing clients with args: {}", Arrays.toString(args));

		EnvironmentEnum environment = determineEnvironment(args);
		
		if (!verifyEnvironment(environment)) {
			return;
		}
		
		Properties configProperties;
		// Try to load the batch properties.
		try {
			configProperties = getProperties(environment);
		}
		catch (IOException e) {
			logger.error("Failed to load properties: ", e);
			return;
		}

		try {
			KeycloakService keycloakService = new KeycloakService(configProperties, environment, determineBatchNumber(args));
			
			keycloakService.initOutput();

			keycloakService.addClients(configProperties, determineNumberOfClients(args), determineClientStartNumber(args));

	    	logger.info("Completed creating clients.");
		} catch (Exception e) {
			logger.error("Creating clients could not be completed due to: " + e.getMessage());
			return;
		}

	}

	/**
	 * Identify the environment from the command-line arguments.
	 * @param args the command-line arguments
	 * @return an EnvironmentEnum identifying the environment
	 */
	private static EnvironmentEnum determineEnvironment(String[] args) {
		EnvironmentEnum environment = DEFAULT_ENV;

		if (args.length >= 1) {
			environment = EnvironmentEnum.valueOf(args[0].toUpperCase());
			logger.info("Running against environment: {}", environment);
		}

		return environment;
	}

	/**
	 * Identify the batch number for this script run.
	 * @param args the command-line arguments
	 * @return the batch number
	 */
	private static String determineBatchNumber(String[] args) {
		Integer batchNumber = 1;

		if (args.length >= 2) {
			batchNumber = Integer.valueOf(args[1]);
			logger.info("Batch number: {}", batchNumber);
		}

		return StringUtils.leftPad(batchNumber.toString(), 4, "0");
	}

	/**
	 * Identify the number of clients to be created from the command-line arguments.
	 * @param args the command-line arguments
	 * @return the number of clients
	 * @throws Exception 
	 */
	private static int determineNumberOfClients(String[] args) throws Exception {
		int numberOfClients = 1;

		if (args.length >= 3) {
			numberOfClients = Integer.valueOf(args[2]);
			if (numberOfClients > NUMBER_OF_CLIENTS_MAX) {
				numberOfClients = 0;
				logger.info("Too many clients requested, must be less than {}, no clients will be created.", NUMBER_OF_CLIENTS_MAX);
			} else if (numberOfClients > NUMBER_OF_CLIENTS_WARN) {
				Scanner reader = new Scanner(System.in);
				System.out.println(String.format("You are creating %d clients. To continue, enter the number of clients requested:", numberOfClients));
				int input = reader.nextInt(); 
				logger.info("Entered: {}", input);
				reader.close();
				
				if (numberOfClients != input) {
					numberOfClients = 0;
					logger.info("Incorrect number entered, no clients will be created.");
				}

			}
			logger.info("Number of clients to be created: {}", numberOfClients);
		}

		return numberOfClients;
	}

	/**
	 * Identify the starting number to use when creating the client IDs from the command-line arguments.
	 * @param args  the command-line arguments
	 * @return the first client ID in the current batch
	 */
	private static int determineClientStartNumber(String[] args) {
		int clientStartNumber = 1;

		if (args.length >= 4) {
			clientStartNumber = Integer.valueOf(args[3]);
			logger.info("Client start number: {}", clientStartNumber);
		}

		return clientStartNumber;
	}

	private static boolean verifyEnvironment(EnvironmentEnum environment) {
		if (environment.equals(EnvironmentEnum.PROD)) {
			Scanner reader = new Scanner(System.in);
			System.out.println(String.format("You are running the script against the Production environment. Type '%s' to continue:", PRODUCTION_ENVIRONMENT_WILL_BE_UPDATED_MSG));
			String input = reader.nextLine(); 
			logger.info("Text entered: {}", input);
			reader.close();
			
			if (!PRODUCTION_ENVIRONMENT_WILL_BE_UPDATED_MSG.equals(input)) {
				logger.info("Correct text not entered");
				return false;
			}
		}
		return true;
	}

	/**
	 * Load the properties for the current batch from the known properties file.
	 * @param environment the current environment
	 * @return a Properties object containing the properties for the current batch
	 * @throws IOException if an error occurs while loading the batch properties
	 */
	private static Properties getProperties(EnvironmentEnum environment) throws IOException {
		// Load the resource file using the ClassLoader.
		InputStream inputStream = Main.class.getClassLoader().getResourceAsStream(String.format(CONFIG_FILE_NAME_TEMPLATE, environment.getValue()));

		// Load the properties from the config file.
		Properties configProperties = new Properties();
		configProperties.load(inputStream);

		return configProperties;
	}
}
