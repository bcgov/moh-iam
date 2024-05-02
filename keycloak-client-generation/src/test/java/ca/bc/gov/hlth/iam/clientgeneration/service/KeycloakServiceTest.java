package ca.bc.gov.hlth.iam.clientgeneration.service;

import static ca.bc.gov.hlth.iam.clientgeneration.service.KeycloakService.OUTPUT_LOCATION_CERTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.security.interfaces.RSAPrivateKey;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ClientCredentialsGrant;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.AccessToken;

import ca.bc.gov.hlth.iam.clientgeneration.Main;
import ca.bc.gov.hlth.iam.clientgeneration.model.csv.ClientCredentials;
import ca.bc.gov.hlth.iam.clientgeneration.utils.KeystoreTools;

public class KeycloakServiceTest {

	private static final Logger logger = LoggerFactory.getLogger(KeycloakServiceTest.class);

	private static final String CONFIG_FILE_NAME_TEMPLATE = "configuration-%s.properties";

	private static final String CONFIG_PROPERTY_URL = "url";

	private static final String CONFIG_PROPERTY_REALM = "realm";

	private static final String CONFIG_PROPERTY_KEYSTORE_FORMAT = "keystore-format";

	private static Properties configProperties;
	
	@BeforeAll
	public static void loadProperties() throws Exception {		
		configProperties = getProperties(EnvironmentEnum.DEV);
	}
	
	@Test
    public void testBulkClientGenerationLoad() throws Exception {

		KeycloakService keycloakService = new KeycloakService(configProperties, EnvironmentEnum.DEV);
	    List<ClientCredentials> clientCredentials = keycloakService.addClients(configProperties, 1, 1);

	    assertEquals(1, clientCredentials.size());
	    
	    keycloakService.processClientsCleanUp(clientCredentials);

	    keycloakService.processClientsCleanUp(clientCredentials);
	}
	
	@Test
    public void testBulkClientGeneration_verify_authentication() throws Exception {

		Properties configProperties = getProperties(EnvironmentEnum.DEV);

		KeycloakService keycloakService = new KeycloakService(configProperties, EnvironmentEnum.DEV);
	    List<ClientCredentials> clientCredentials = keycloakService.addClients(configProperties, 1, 20);

	    assertEquals(1, clientCredentials.size());
	    
        URI tokenEndpoint = new URI(configProperties.getProperty(CONFIG_PROPERTY_URL) + "/realms/" + configProperties.getProperty(CONFIG_PROPERTY_REALM) + "/protocol/openid-connect/token");

        // Construct the client credentials grant type
        AuthorizationGrant clientGrant = new ClientCredentialsGrant();

        // Get the client authentication method
        ClientAuthentication clientAuthentication = buildAuthenticationMethod(clientCredentials.get(0), tokenEndpoint);

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
            System.out.println("HTTP status: " + statusCode);  
      
            /* Note, the response returned depends on PPM API app PharmaNet's endpoint so this test result may vary but it's purpose is to determine if
             * authentication was succcessful, this can be confirmed by finding the log entry:
             *  "Informational: HL7v2 Authorization Success! Scope(s) provided are correct for the HL7v2 message"
             *  in the Pod for the Patient service at https://console.apps.silver.devops.gov.bc.ca/k8s/ns/2f77cb-dev/deploymentconfigs/patientservice-dev-ppmservice.
             */            
        }  
        catch (InterruptedException | IOException e) {  
            throw new RuntimeException(e);  
        }  
	    
	    keycloakService.processClientsCleanUp(clientCredentials);
    }
	
    public ClientAuthentication buildAuthenticationMethod(ClientCredentials clientCredentials, URI tokenEndpoint) throws Exception {
        File certFile = new File(OUTPUT_LOCATION_CERTS + clientCredentials.getCertFileName());
        KeyStore keyStore = KeystoreTools.loadKeyStore(certFile, clientCredentials.getStorePassword(), configProperties.getProperty(CONFIG_PROPERTY_KEYSTORE_FORMAT));
        RSAPrivateKey privateKey = (RSAPrivateKey) keyStore.getKey(clientCredentials.getCertAlias(), clientCredentials.getStorePassword().toCharArray());
        try {
            ClientID clientID = new ClientID(clientCredentials.getClientId());
            return new PrivateKeyJWT(clientID, tokenEndpoint, JWSAlgorithm.RS256, privateKey, null, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    private static final String SAMPLE_MESSAGE_MEDICATION_STATEMENT = "{\r\n"
    		+ "    \"resourceType\": \"DocumentReference\",\r\n"
    		+ "    \"masterIdentifier\": {\r\n"
    		+ "        \"system\": \"urn:ietf:rfc:3986\",\r\n"
    		+ "        \"value\": \"urn:uuid:D8196F60-8E3F-40A6-B5C8-B5680B2C21EC\"\r\n"
    		+ "    },\r\n"
    		+ "    \"status\": \"current\",\r\n"
    		+ "    \"date\": \"2020-07-30T01:09:57Z\",\r\n"
    		+ "    \"content\": [{\r\n"
    		+ "        \"attachment\": {\r\n"
    		+ "            \"contentType\": \"x-application/hl7-v2+er7\",\r\n"
    		+ "            \"data\": \"TVNIfF5+XCZ8REVTS1RPUHxCQzAxMDAwMDA2fFBOUHxQUHx8TUE6MjQuODUuMTQwLjkwfFpQTnwwMjYwOTB8RHwyLjENWlpafFRSUHx8MDI2MDkwfFAxfFhYQktSfHx8DVpDQXwwMDAwMDF8MDN8MDB8QVJ8MDQNWkNCfEJDMDAwMDAxQUJ8MjQwMzA1fDAyNjA5MA1aQ0N8fHx8fHx8fHx8MDAwOTczNTM5MTQxOXwNDQ==\"\r\n"
    		+ "        }\r\n"
    		+ "    }]\r\n"
    		+ "}";
    
    private static final String SAMPLE_MESSAGE_PATIENT = "{\r\n"
    		+ "    \"resourceType\": \"DocumentReference\",\r\n"
    		+ "    \"masterIdentifier\": {\r\n"
    		+ "        \"system\": \"urn:ietf:rfc:3986\",\r\n"
    		+ "        \"value\": \"urn:uuid:D8196F60-8E3F-40A6-B5C8-B5680B2C21EC\"\r\n"
    		+ "    },\r\n"
    		+ "    \"status\": \"current\",\r\n"
    		+ "    \"date\": \"2020-07-30T01:09:57Z\",\r\n"
    		+ "    \"content\": [{\r\n"
    		+ "        \"attachment\": {\r\n"
    		+ "            \"contentType\": \"x-application/hl7-v2+er7\",\r\n"
    		+ "            \"data\": \"TVNIfF5+XFwmfDEyMzQ1Njd8MTIzNDU2N3x8RVJYUFB8fHVzZXJJRDoxOTIuMTY4LjAuMXxaUE58NDQzNzY5fFB8Mi4xfHwKWkNBfHw3MHwwMHxBUnwwNXwKWkNCfEJDMDAwMDBJMTB8MTQwNzE1fDQ0Mzc2OQpaQ0N8fHx8fHx8fHx8MDAwbm5ubm5ubm5ubnwKWlpafFRJRHx8NDQzNzY5fFAxfG5ubm5ubm5ubm58fHw=\"\r\n"
    		+ "        }\r\n"
    		+ "    }]\r\n"
    		+ "}";
    
    
}
