package ca.bc.gov.hlth.iam.clientgeneration.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.ClientAttributeCertificateResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.KeyStoreConfig;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

import ca.bc.gov.hlth.iam.clientgeneration.model.csv.ClientCredentials;
import ca.bc.gov.hlth.iam.clientgeneration.utils.KeystoreTools;

public class KeycloakService {

	private static final Logger logger = LoggerFactory.getLogger(KeycloakService.class);

	private static final String BATCH_NUMBER = "01";

	private static final String OUTPUT_LOCATION = "C:\\tmp\\ppm\\batch" + BATCH_NUMBER;
	
	public static final String OUTPUT_LOCATION_CERTS = OUTPUT_LOCATION + "\\certs\\";
	
	private static final String OUTPUT_FILE = OUTPUT_LOCATION + "\\client_information_" + BATCH_NUMBER + ".csv";

	private static final String CLIENT_ID_BASE = "aaappm-api-";

	private static final String CLIENT_NAME_BASE = "aaaPPM API ";

	private static final String CLIENT_DESCRIPTION = "Batch generated client for use with clients that wish to onboard to using PPM API.";

	private static final String CONFIG_PROPERTY_URL = "url";

	private static final String CONFIG_PROPERTY_REALM = "realm";

	private static final String CONFIG_PROPERTY_CLIENT_ID = "client-id";

	private static final String CONFIG_PROPERTY_USERNAME = "username";

	private static final String CONFIG_PROPERTY_SCOPES = "scopes";

	private static final String CONFIG_PROPERTY_KEYSTORE_FORMAT = "keystore-format";

	private static final String AUTH_TYPE_CLIENT_JWT = "client-jwt";

	private static final String JWT_CREDENTIAL = "jwt.credential";

	private static final String FILE_EXTENSION_PFX = ".pfx";

	private static final String FILE_EXTENSION_JKS = ".jks";

	private static final String KEYCLOAK_FORMAT_PKCS12 = "PKCS12";

	private static final String KEYCLOAK_FORMAT_JKS = "JKS";

	private String realm;

	private String keystoreFormat = "PKCS12";
	
	private String fileExtension;

	private RealmResource realmResource;

	public KeycloakService(Properties configProperties, EnvironmentEnum environment) throws Exception {
		super();
		init(configProperties, environment);
	}

	public void init(Properties configProperties, EnvironmentEnum environment) throws Exception {
		logger.info("Initializing Keycloak connection against: {}", configProperties.getProperty(CONFIG_PROPERTY_URL));
		realm = configProperties.getProperty(CONFIG_PROPERTY_REALM);
		logger.info("Using Realm: {}", realm);

		Keycloak keycloak = KeycloakBuilder.builder().serverUrl(configProperties.getProperty(CONFIG_PROPERTY_URL))
				.realm(realm).grantType(OAuth2Constants.PASSWORD)
				.clientId(configProperties.getProperty(CONFIG_PROPERTY_CLIENT_ID)) //
				.username(configProperties.getProperty(CONFIG_PROPERTY_USERNAME)).password(getUserPassword(environment))
				.build();

		realmResource = keycloak.realm(realm);

		if (StringUtils.isNoneBlank(configProperties.getProperty(CONFIG_PROPERTY_KEYSTORE_FORMAT))) {
			keystoreFormat = configProperties.getProperty(CONFIG_PROPERTY_KEYSTORE_FORMAT);
		};
		
		fileExtension = determineFileExtenstion();
		
		initOutput();
		
		logger.info("Keycloak connection initialized.");
	}

	private static String getUserPassword(EnvironmentEnum environment) {
		return System.getenv(environment.getPasswordKey());
	}

	public List<ClientCredentials> addClients(Properties configProperties, int numberOfClients, int clientStartNumber) {
		
		logger.info("Begin addClients...");
		
		ClientsResource clientsResource = realmResource.clients();
	
		List<ClientCredentials> ccs = new ArrayList<>();

		
		List<String> clientScopes = retrieveClientScopes(configProperties);
		
		for (int i=0; i<numberOfClients;i++) {
			ClientCredentials clientCredentials = addClient(clientsResource, clientScopes, clientStartNumber + i);
			if (clientCredentials != null) {
				ccs.add(clientCredentials);
			}
		}
		
		writeCsvFromBean(ccs);

		logger.info("End addClients.");
		return ccs;
		
	}
	
	public ClientCredentials addClient(ClientsResource clientsResource, List<String> scropes, int clientIteration) {
		
		logger.info("Begin addingClient...");
		
		String clientSuffix = StringUtils.leftPad(Integer.toString(clientIteration), 8, "0"); 
		logger.info("Usign clientSuffix: {}", clientSuffix);
		
		String clientId = CLIENT_ID_BASE + clientSuffix;
		String clientName = CLIENT_NAME_BASE + clientSuffix;
		logger.info("Creating: {} : {}", clientId, clientName);

		ClientRepresentation cr = new ClientRepresentation();
		cr.setClientId(clientId);
		cr.setName(clientName);
		cr.setDescription(CLIENT_DESCRIPTION);

		populateDefaultsClientRepresentation(cr,scropes);

		processClientCleanUp(cr.getClientId(), clientsResource);
		
		try {
			if(existsClient(clientId, clientsResource)) {
				logger.warn("Client {} already exists", clientId);
				return null;
			}
			Response response = clientsResource.create(cr);
	
			logger.info("Status: {}", response.getStatus());
			if (response.getStatus() != 201) {
				throw new Exception(response.getStatusInfo().getReasonPhrase());
			}
	
			ClientRepresentation clientRepresentation = retrieveClient(clientsResource, cr.getClientId());			
			ClientResource clientResource = clientsResource.get(clientRepresentation.getId());
			if (clientResource == null) {
				throw new Exception("New Client not found");
			}
	
			ClientAttributeCertificateResource certficateResource = clientResource.getCertficateResource(JWT_CREDENTIAL);
			KeyStoreConfig keyStoreConfig = createKeyStoreConfig(clientId);
			byte[] keyStoreBytes = certficateResource.generateAndGetKeystore(keyStoreConfig);
	
			saveKeyStoreAsCertificate(keyStoreBytes, clientId, keyStoreConfig.getStorePassword());
	
			ClientCredentials cc = createClientCredentials(clientId, keyStoreConfig);
			logger.info(cc.toString());
			
			return cc;
			
		} catch (Exception e) {
			logger.error("Exception occurred during client creation. Stopping processing and cleaning up client if necessary");
			processClientCleanUp(clientId, clientsResource);
		}
		return null;
	}

	private void populateDefaultsClientRepresentation(ClientRepresentation cr, List<String> scropes) {
		cr.setEnabled(true);

		cr.setPublicClient(false); // Client authentication
		cr.setAuthorizationServicesEnabled(false); // Authorization

		cr.setStandardFlowEnabled(false); // Standard flow
		cr.setImplicitFlowEnabled(false); // Implicit flow
		cr.setDirectAccessGrantsEnabled(false); // Direct access grants
		cr.setServiceAccountsEnabled(true); // Service accounts roles

		cr.setFrontchannelLogout(false);

		cr.setClientAuthenticatorType(AUTH_TYPE_CLIENT_JWT); // Client Authenticator

//		Configure client scopes by setting default scopes. This removes any predetermined defaults by overwriting them with those in the provided list.
		cr.setDefaultClientScopes(scropes);

		/* Add an Audience mapper */
		cr.setProtocolMappers(createAudienceProtocolMapper());
	}

	private ClientCredentials createClientCredentials(String clientId, KeyStoreConfig kc) {
		ClientCredentials cc = new ClientCredentials();
		cc.setClientId(clientId);
		cc.setCertFileName(kc.getKeyAlias() + fileExtension);
		cc.setCertAlias(kc.getKeyAlias());
		cc.setKeyPassword(kc.getKeyPassword());
		cc.setStorePassword(kc.getStorePassword());
		return cc;
	}

	private void saveKeyStoreAsCertificate(byte[] keyStoreBytes, String clientId, String keystorePassword) throws Exception {
		ByteArrayInputStream keyStoreIs = new ByteArrayInputStream(keyStoreBytes);
		KeyStore keyStore = null;
		try {
			keyStore = KeystoreTools.loadKeyStore(keyStoreIs, keystorePassword, keystoreFormat);
			keyStoreIs.close();
			File f = new File(OUTPUT_LOCATION_CERTS + clientId + fileExtension);
			FileOutputStream fos = new FileOutputStream(f);
			keyStore.store(fos, keystorePassword.toCharArray());
		} catch (Exception e) {
			logger.error("Error saving KeyStore file: {}", e.getMessage());
			throw e;
		}
	}

	private KeyStoreConfig createKeyStoreConfig(String clientId) {
		KeyStoreConfig keyStoreConfig = new KeyStoreConfig();

		//TODO Restrict password chars if they cause an issue
		String password = RandomStringUtils.randomAscii(16);
		logger.info("ClienID: {} : Password {}", clientId, password);

		keyStoreConfig.setRealmCertificate(true);
		keyStoreConfig.setRealmAlias(realm);
		keyStoreConfig.setKeyAlias(clientId);
		keyStoreConfig.setKeyPassword(password);
		keyStoreConfig.setStorePassword(password);
		keyStoreConfig.setFormat(keystoreFormat);
		return keyStoreConfig;
	}

	/*
	 * Creates an Audience Mapper
	 */
	private List<ProtocolMapperRepresentation> createAudienceProtocolMapper() {

//		Mapper config from existing client with Audience mapper
//		Protocol: openid-connect
//		ProtocolMapper: oidc-audience-mapper
//		ConsentText: null
//		ID: 590d6374-6f6b-4870-b1ac-4d0856262eb3
//		Name: Pharmanet Audience
//		Key: id.token.claim; Value: false
//		Key: access.token.claim; Value: true
//		Key: included.custom.audience; Value: pharmanet

		ProtocolMapperRepresentation pmr = new ProtocolMapperRepresentation();
		pmr.setName("Pharmanet Audience");
		pmr.setProtocol("openid-connect");
		pmr.setProtocolMapper("oidc-audience-mapper");

		Map<String, String> audienceMapperConfig = new HashMap<>();
		audienceMapperConfig.put("included.custom.audience", "pharmanet");
		audienceMapperConfig.put("id.token.claim", "false");
		audienceMapperConfig.put("access.token.claim", "true");
		pmr.setConfig(audienceMapperConfig);

		List<ProtocolMapperRepresentation> pmrs = new ArrayList<>();
		pmrs.add(pmr);
		
		return pmrs;
	}

	/*
	 * Retrieve the client scopes from the properties file
	 */
	private List<String> retrieveClientScopes(Properties configProperties) {
		String scopes = configProperties.getProperty(CONFIG_PROPERTY_SCOPES);
		logger.info("Scopes: {}", scopes);
		String[] scopesArray = StringUtils.split(scopes.strip().replace(" ", ""), ",");
		return Arrays.asList(scopesArray);
	}

	public void writeCsvFromBean(List<ClientCredentials> sampleData) {

		Path path = Paths.get(OUTPUT_FILE);
		try (Writer writer = new FileWriter(path.toString())) {

			StatefulBeanToCsv<ClientCredentials> sbc = new StatefulBeanToCsvBuilder<ClientCredentials>(writer)
					.withQuotechar('\'').withSeparator(CSVWriter.DEFAULT_SEPARATOR).build();

			sbc.write(sampleData);
		} catch (IOException | CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
			logger.error("Error writing client details", e.getMessage());
		}
	}

	public void processClientsCleanUp(List<ClientCredentials> clientCredentials) {
		
		ClientsResource clientsResource = realmResource.clients();
		
		clientCredentials.forEach(cc -> {
			processClientCleanUp(cc.getClientId(), clientsResource);
		});
	}

	public void processClientCleanUp(String clientId, ClientsResource clientsResource) {
		ClientRepresentation clientRepresentation = retrieveClient(clientsResource, clientId);
		if (clientRepresentation != null) {
			printClientDetails(clientRepresentation);
			deleteClient(clientsResource, clientRepresentation.getId());
			deleteKeystore(clientId);
		}
	}

	private void deleteKeystore(String clientId) {
		File f = new File(OUTPUT_LOCATION_CERTS + clientId + fileExtension);
		logger.info("Deleting {}", f.getAbsolutePath());
		boolean deleted = f.delete();
		if (deleted) {
			logger.info("Deleted {}", f.getAbsolutePath());
		} else {
			logger.info("Failed to delete {}", f.getAbsolutePath());
		}
	}

	private ClientRepresentation retrieveClient(ClientsResource clientsResource, String clientId) {
		List<ClientRepresentation> clientRepresentations = clientsResource.findByClientId(clientId);
		if (clientRepresentations.size() == 1) {
			return clientRepresentations.get(0);
		}
		return null;
	}

	private boolean existsClient(String clientId, ClientsResource clientsResource) {
		return retrieveClient(clientsResource, clientId) != null;
	}

	private void deleteClient(ClientsResource clientsResource, String id) {
		logger.info("Removing client ID: {}", id);
		ClientResource clientResource = clientsResource.get(id);
		clientResource.remove();
		logger.info("Removed client ID: {}", id);
	}

	private void printClientDetails(ClientRepresentation clientRepresentation) {
		List<ProtocolMapperRepresentation> protocolMapperRepresentations = clientRepresentation
				.getProtocolMappers();
		protocolMapperRepresentations.forEach(pm -> {
			logger.debug("Protocol: {}", pm.getProtocol());
			logger.debug("ProtocolMapper: {}", pm.getProtocolMapper());
			logger.debug("ID: {}", pm.getId());
			logger.debug("Name: {}", pm.getName());
			Map<String, String> config = pm.getConfig();
			config.forEach((key, value) -> {
				logger.debug("Config Key: {}; Value: {}", key, value);
			});
		});

		logger.debug("Client Rep getClientAuthenticatorType: {}",
				clientRepresentation.getClientAuthenticatorType());
		logger.debug("Client Rep AuthorizationSettings: {}", clientRepresentation.getAuthorizationSettings());
		logger.debug("Client Rep RegistrationAccessToken: {}",
				clientRepresentation.getRegistrationAccessToken());
		logger.debug("Client Rep AuthorizationServicesEnabled: {}",
				clientRepresentation.getAuthorizationServicesEnabled());
		logger.debug("Client Rep Protocol: {}", clientRepresentation.getProtocol());

		Map<String, String> attributes = clientRepresentation.getAttributes();
		attributes.forEach((k, v) -> {
			logger.debug("Attribute Key: {}; Value: {}", k, v);
		});
	}

	private void initOutput() throws Exception {
		logger.debug("Initializing file location.");			
		File fileLocation = new File(OUTPUT_LOCATION_CERTS);
		if (!fileLocation.exists()) {
			logger.debug("File location directory [{}] did not exist so it will be created.", OUTPUT_LOCATION_CERTS);			
			boolean dirCreated = fileLocation.mkdirs();
			if (!dirCreated) {
				logger.error("Failed to initialize output dir: {}", fileLocation.getAbsolutePath());
				throw new Exception("Failed to initialize output dir");
			}
		}
		logger.debug("{} - File location is [{}].", fileLocation.getAbsolutePath());			
	}

	private String determineFileExtenstion() {
		switch (keystoreFormat) {
		case KEYCLOAK_FORMAT_PKCS12: return FILE_EXTENSION_PFX;
		case KEYCLOAK_FORMAT_JKS: return FILE_EXTENSION_JKS;
		default: return null;
		}
	}

}
