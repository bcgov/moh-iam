package ca.bc.gov.hlth.iam.clientgeneration;

import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;
import java.net.URISyntaxException;
import java.net.URL;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

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
 *     - cert file name
 *     - cert file alias
 *     - key password
 *     - store password
 *     - cert expiry date
 * The certificates will also be genearated and saved locally.
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static final EnvironmentEnum DEFAULT_ENV = EnvironmentEnum.DEV;

    private static final String CONFIG_FILE_NAME_TEMPLATE = "configuration-%s.properties";

    public static void main(String[] args) {
        logger.info("Begin processing clients with args: {}", Arrays.toString(args));

        EnvironmentEnum environment = determineEnvironment(args);
        Properties configProperties = getProperties(environment);

        KeycloakService keycloakService;

        try {
            keycloakService = new KeycloakService(configProperties, environment);
        }
        catch (Exception e) {
            logger.error("Failed to create Keycloak service: ", e);
            logger.error("Abort.");
            return;
        }

        keycloakService.addClients(configProperties, determineNumberOfClients(args), determineClientStartNumber(args));
        logger.info("Completed creating clients.");
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
     * Identify the number of clients to be created from the command-line arguments.
     * @param args the command-line arguments
     * @return the number of clients
     */
    private static int determineNumberOfClients(String[] args) {
        int numberOfClients = 1;

        if (args.length >= 2) {
            numberOfClients = Integer.valueOf(args[1]);
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

        if (args.length >= 3) {
            clientStartNumber = Integer.valueOf(args[2]);
            logger.info("Client start number: {}", clientStartNumber);
        }

        return clientStartNumber;
    }

    /**
     * Load the properties for the current batch from the known properties file.
     * @param environment the current environment
     * @return a Properties object containing the properties for the current batch
     */
    private static Properties getProperties(EnvironmentEnum environment) {
        // Access the config file resource.
        URL defaultLocation = Main.class.getClassLoader().getResource(String.format(CONFIG_FILE_NAME_TEMPLATE, environment.getValue()));
        String configPath;

        // Try to determine the config file's absolute path.
        try {
            configPath = new File(defaultLocation.toURI()).getAbsolutePath();
        }
        catch (URISyntaxException e) {
            logger.error("Failed to load config file: ", e);
            return null;
        }

        File file = new File(configPath);
        InputStream inputStream;

        // Try to open the config file as an InputStream.
        try {
            inputStream = file.exists() ? new FileInputStream(file) : Main.class.getResourceAsStream(configPath);
        }
        catch (FileNotFoundException e) {
            logger.error("Failed to load config file: ", e);
            return null;
        }

        Objects.requireNonNull(inputStream, String.format("Configuration file not found at '%s'.", configPath));
        Properties configProperties = new Properties();

        // Try to load the properties from the config file.
        try {
            configProperties.load(inputStream);
        }
        catch (IOException e) {
            logger.error("Failed to load config file: ", e);
            return null;
        }

        return configProperties;
    }
}
