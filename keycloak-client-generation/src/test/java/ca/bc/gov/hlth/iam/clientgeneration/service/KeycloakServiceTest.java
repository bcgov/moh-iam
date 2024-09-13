package ca.bc.gov.hlth.iam.clientgeneration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ClientCredentialsGrant;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.AccessToken;

import ca.bc.gov.hlth.iam.clientgeneration.model.csv.ClientCredentials;
import ca.bc.gov.hlth.iam.clientgeneration.utils.KeystoreTools;

/**
 * Run automated tests to validate the functionality of the bulk Keycloak client
 * generation script and to validate proper connectivity before attempting an
 * actual batch job.
 */
public class KeycloakServiceTest {

	private static final Logger logger = LoggerFactory.getLogger(KeycloakServiceTest.class);

	private static final String CONFIG_FILE_NAME_TEMPLATE = "configuration-%s.properties";

	private static final String CONFIG_PROPERTY_URL = "url";

	private static final String CONFIG_PROPERTY_REALM = "realm";

	private static final String CONFIG_PROPERTY_KEYSTORE_FORMAT = "keystore-format";

	private static final String CONFIG_PROPERTY_OUTPUT_LOCATION = "output-location";

	private static String configFileName;

	private static Properties configProperties;

	/**
	 * Load the confugiration properties from the configuration properties file
	 * before executing any tests.
	 * @throws IOException if the resource doesn't exist or can't be loaded
	 */
	@BeforeAll
	public static void loadProperties() throws IOException {
		// Determine the file name from the name pattern and the environment.
		configFileName = String.format(CONFIG_FILE_NAME_TEMPLATE, EnvironmentEnum.DEV.getValue());

		// Open the properties file as a stream.
		InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(configFileName);

		// Load the properties from the file.
		configProperties = new Properties();
		configProperties.load(inputStream);
	}

	/**
	 * Create a batch of clients and remove them when done.
	 * @throws Exception if an error occurs during the creation of the Keycloak actor
	 */
	@Disabled("System test to be run only when load testing the bulk generation")
	@Test
	public void testBulkClientGenerationLoad() throws Exception {
		int numberOfClientsToCreate = 500;

		// Initialize the Keycloak actor.
		KeycloakService keycloakService = new KeycloakService(configProperties, EnvironmentEnum.DEV, "0001");

		// Create the clients and retrieve their credentials.
		List<ClientCredentials> clientCredentials = keycloakService.addClients(configProperties, numberOfClientsToCreate, 1);

		// Validate the number of clients created.
		assertEquals(numberOfClientsToCreate, clientCredentials.size(), "Incorrect number of client credentials returned.");

		// Remove the clients.
		keycloakService.processClientsCleanUp(clientCredentials);
	}

	/**
	 * Create a new client and use its credentials to send a test transaction to
	 * the PNET API. The success of the transaction can be validated manually,
	 * if desired, in the PNET API's logs.
	 * @throws Exception if an error occurs during the creation of the Keycloak actor
	 * @throws URISyntaxException if the Keycloak token endpoint can't be created due to a URI syntax error
	 * @throws GeneralSecurityException if the key store cannot be loaded due to an issue with the system's certificate algorithms or with the key store's certificates
	 * @throws IOException if an error occurs while loading the key store from the certificate file or if the HTTP request couldn't be made
	 * @throws JOSEException if the JWT could not be signed
	 * @throws ParseException if the HTTP response couldn't be parsed to a token response
	 */
	@Disabled("System test to be run only when end to end testing client generation")
	@Test
	public void testBulkClientGeneration_verify_authentication() throws Exception, URISyntaxException, GeneralSecurityException, IOException, JOSEException, ParseException {
	    int clientStartNumber = 509;
	    String batchNumber = "0001";
	    List<ClientCredentials> clientCredentials = new ArrayList<>();

		KeycloakService keycloakService = new KeycloakService(configProperties, EnvironmentEnum.DEV, batchNumber);		
		
		ClientCredentials cc = new ClientCredentials();
		cc.setClientId(KeycloakService.CLIENT_ID_BASE + keycloakService.generateClientIDSuffix(clientStartNumber));
		clientCredentials.add(cc);
		
		keycloakService.cleanUp(clientCredentials);
		keycloakService.initOutput();		
		
		try {
			clientCredentials = keycloakService.addClients(configProperties, 1, clientStartNumber);
	
		    assertEquals(1, clientCredentials.size());
		    
	        URI tokenEndpoint = new URI(configProperties.getProperty(CONFIG_PROPERTY_URL) + "/realms/" + configProperties.getProperty(CONFIG_PROPERTY_REALM) + "/protocol/openid-connect/token");
	
	        // Construct the client credentials grant type
	        AuthorizationGrant clientGrant = new ClientCredentialsGrant();
	
	        // Get the client authentication method
	        ClientAuthentication clientAuthentication = buildAuthenticationMethod(batchNumber, clientCredentials.get(0), tokenEndpoint);
	
	        // Make the token request
	        Scope requiredScopes = new Scope("openid system/*.write system/*.read system/Claim.read system/Claim.write system/Patient.read system/Patient.write".split(" "));
	        TokenRequest tokenRequest = new TokenRequest(tokenEndpoint, clientAuthentication, clientGrant, requiredScopes);
	
	        TokenResponse tokenResponse = TokenResponse.parse(tokenRequest.toHTTPRequest().send());
	
	        // Check if we got a 2xx response back from the server
	        assertTrue(tokenResponse.indicatesSuccess());
	
	        AccessTokenResponse successResponse = tokenResponse.toSuccessResponse();
	
	        // Get the access token and set the expiry time
	        AccessToken accessToken = successResponse.getTokens().getAccessToken();
	        
	        logger.info("Token: {}", accessToken.getValue());
		    
	        String authorizationHeader = accessToken.toAuthorizationHeader();
	
	        HttpRequest.BodyPublisher messagePublisher = HttpRequest.BodyPublishers.ofString(SAMPLE_MESSAGE_PATIENT);  
	        
	        HttpClient httpClient = HttpClient.newHttpClient();  
	        HttpRequest request = HttpRequest  
	//                .newBuilder(URI.create("https://pnet-dev.api.gov.bc.ca/api/v1/Claim"))
	                .newBuilder(URI.create("https://pnet-dev.api.gov.bc.ca/api/v1/Patient"))
	//                .newBuilder(URI.create("https://pnet-dev.api.gov.bc.ca/api/v1/MedicationStatement"))
	                .setHeader("Authorization", authorizationHeader)
	                .POST(messagePublisher)  
	                .setHeader("Content-Type", "application/json")  
	                .build();  
	      
	        try {  
	            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());  
	      
	            int statusCode = response.statusCode();
	            logger.info("Service call response HTTP status: {}", statusCode);  
	      
	            /* Note, the response returned depends on PPM API app PharmaNet's endpoint so this test result may vary but it's purpose is to determine if
	             * authentication was succcessful, this can be confirmed by finding the log entry:
	             *  "Informational: HL7v2 Authorization Success! Scope(s) provided are correct for the HL7v2 message"
	             *  in the Pod for the Patient service at https://console.apps.silver.devops.gov.bc.ca/k8s/ns/2f77cb-dev/deploymentconfigs/patientservice-dev-ppmservice.
	             */            
	        }  
	        catch (InterruptedException | IOException e) {
	        	logger.error("batchNumberHTTP status: {}", e.getMessage());  
	            throw new RuntimeException(e);  
	        } 
	        
	        //Clean up all generated items after test run. Can be commented out if you wish to inspect the generated items.
	    	keycloakService.cleanUp(clientCredentials);
		}
	    catch (Exception ex) {
	    	keycloakService.cleanUp(clientCredentials);
	    	fail("Test failed due to " + ex.getMessage(), ex.getCause());
	    }
	}

	/**
	 * Use the given client credentials to create and sign a JWT suitable for
	 * authentication to the PNET API.
	 * @param batchNumber 
	 * @param clientCredentials the credentials of the client that will be used for authentication
	 * @param tokenEndpoint the URI of the Keycloak token endpoint that will distribute the access token
	 * @return a signed JWT with a five-minute lifetime
	 * @throws GeneralSecurityException if the key store cannot be loaded due to an issue with the system's certificate algorithms or with the key store's certificates
	 * @throws IOException if an error occurs while loading the key store from the certificate file
	 * @throws JOSEException if the JWT could not be signed
	 */
	public ClientAuthentication buildAuthenticationMethod(String batchNumber, ClientCredentials clientCredentials, URI tokenEndpoint) throws GeneralSecurityException, IOException, JOSEException {
		// Access the client's certificate file.
        File certFile = new File(configProperties.getProperty(CONFIG_PROPERTY_OUTPUT_LOCATION) + "\\" + batchNumber + "\\certs\\" + clientCredentials.getCertFilename());

		// Unlock and load the key store from the certificate file.
		KeyStore keyStore = KeystoreTools.loadKeyStore(certFile, clientCredentials.getStorePassword(), configProperties.getProperty(CONFIG_PROPERTY_KEYSTORE_FORMAT));

		// Unlock and load the private key component.
		RSAPrivateKey privateKey = (RSAPrivateKey) keyStore.getKey(clientCredentials.getCertAlias(), clientCredentials.getStorePassword().toCharArray());

		// Generate the signed JWT to authenticate to the PNET API.
		ClientID clientID = new ClientID(clientCredentials.getClientId());
		return new PrivateKeyJWT(clientID, tokenEndpoint, JWSAlgorithm.RS256, privateKey, null, null);
	}

	private static final String SAMPLE_MESSAGE_PATIENT = "{\r\n"
			+ "	\"resourceType\": \"DocumentReference\",\r\n"
			+ "	\"masterIdentifier\": {\r\n"
			+ "		\"system\": \"urn:ietf:rfc:3986\",\r\n"
			+ "		\"value\": \"urn:uuid:D8196F60-8E3F-40A6-B5C8-B5680B2C21EC\"\r\n"
			+ "	},\r\n"
			+ "	\"status\": \"current\",\r\n"
			+ "	\"date\": \"2020-07-30T01:09:57Z\",\r\n"
			+ "	\"content\": [{\r\n"
			+ "		\"attachment\": {\r\n"
			+ "			\"contentType\": \"x-application/hl7-v2+er7\",\r\n"
			+ "			\"data\": \"TVNIfF5+XFwmfDEyMzQ1Njd8MTIzNDU2N3x8RVJYUFB8fHVzZXJJRDoxOTIuMTY4LjAuMXxaUE58NDQzNzY5fFB8Mi4xfHwKWkNBfHw3MHwwMHxBUnwwNXwKWkNCfEJDMDAwMDBJMTB8MTQwNzE1fDQ0Mzc2OQpaQ0N8fHx8fHx8fHx8MDAwbm5ubm5ubm5ubnwKWlpafFRJRHx8NDQzNzY5fFAxfG5ubm5ubm5ubm58fHw=\"\r\n"
			+ "		}\r\n"
			+ "	}]\r\n"
			+ "}";

	private static final String SAMPLE_MESSAGE_MEDICATION_STATEMENT = "{\r\n"
			+ "	\"resourceType\": \"DocumentReference\",\r\n"
			+ "	\"masterIdentifier\": {\r\n"
			+ "		\"system\": \"urn:ietf:rfc:3986\",\r\n"
			+ "		\"value\": \"urn:uuid:D8196F60-8E3F-40A6-B5C8-B5680B2C21EC\"\r\n"
			+ "	},\r\n"
			+ "	\"status\": \"current\",\r\n"
			+ "	\"date\": \"2020-07-30T01:09:57Z\",\r\n"
			+ "	\"content\": [{\r\n"
			+ "		\"attachment\": {\r\n"
			+ "			\"contentType\": \"x-application/hl7-v2+er7\",\r\n"
			+ "			\"data\": \"TVNIfF5+XCZ8REVTS1RPUHxCQzAxMDAwMDA2fFBOUHxQUHx8TUE6MjQuODUuMTQwLjkwfFpQTnwwMjYwOTB8RHwyLjENWlpafFRSUHx8MDI2MDkwfFAxfFhYQktSfHx8DVpDQXwwMDAwMDF8MDN8MDB8QVJ8MDQNWkNCfEJDMDAwMDAxQUJ8MjQwMzA1fDAyNjA5MA1aQ0N8fHx8fHx8fHx8MDAwOTczNTM5MTQxOXwNDQ==\"\r\n"
			+ "		}\r\n"
			+ "	}]\r\n"
			+ "}";

}
