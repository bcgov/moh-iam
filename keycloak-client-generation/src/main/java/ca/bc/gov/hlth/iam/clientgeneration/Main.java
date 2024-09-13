package ca.bc.gov.hlth.iam.clientgeneration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

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

	public static void main(String[] args) {
		logger.info("Begin processing clients with args: {}", Arrays.toString(args));

		EnvironmentEnum environment = determineEnvironment(args);
		Properties configProperties;

		// Try to load the batch properties.
		try {
			configProperties = getProperties(environment);
		}
		catch (IOException e) {
			logger.error("Failed to load properties: ", e);
			logger.error("Abort.");
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
	 */
	private static int determineNumberOfClients(String[] args) {
		int numberOfClients = 1;

		if (args.length >= 3) {
			numberOfClients = Integer.valueOf(args[2]);
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
