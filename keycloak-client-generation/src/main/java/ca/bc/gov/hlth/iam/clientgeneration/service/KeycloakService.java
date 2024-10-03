package ca.bc.gov.hlth.iam.clientgeneration.service;

import static org.keycloak.common.util.KeystoreUtil.KeystoreFormat.PKCS12;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.RandomStringGenerator;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.ClientAttributeCertificateResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.KeystoreUtil.KeystoreFormat;
import org.keycloak.representations.KeyStoreConfig;
import org.keycloak.representations.idm.CertificateRepresentation;
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
import ca.bc.gov.hlth.iam.clientgeneration.model.csv.CustomColumnPositionStrategy;

public class KeycloakService {

	private static final Logger logger = LoggerFactory.getLogger(KeycloakService.class);

	public static final String CLIENT_ID_BASE = "ppm-api-BC";

	private static final String CLIENT_NAME_BASE = "PPM API ";

	private static final String CLIENT_DESCRIPTION = "Batch generated client for use with clients that wish to onboard to using PPM API.";

	// TODO Move config props to common file
	private static final String CONFIG_PROPERTY_URL = "url";

	private static final String CONFIG_PROPERTY_REALM = "realm";

	private static final String CONFIG_PROPERTY_CLIENT_ID = "client-id";

	private static final String CONFIG_PROPERTY_SCOPES = "scopes";

	private static final String CONFIG_PROPERTY_KEYSTORE_FORMAT = "keystore-format";

	private static final String CONFIG_PROPERTY_OUTPUT_LOCATION = "output-location";

	private static final String AUTH_TYPE_CLIENT_JWT = "client-jwt";

	private static final String JWT_CREDENTIAL = "jwt.credential";

	private static final String FILE_EXTENSION_PFX = ".pfx";

	private static final String FILE_EXTENSION_JKS = ".jks";

	private static final String FILE_EXTENSION_CRT = ".crt";

	private static final String PASSWORD_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&";

	private static final String MOH_APPLICATIONS = "moh_applications";

	private String realm;

	// default to PKCS12
	private String keystoreFormat = PKCS12.toString();

	private String fileExtension;

	private RealmResource realmResource;

	private String batchNumber;

	private String outputLocation;

	private String outputLocationCerts;

	private String outputLocationX509Certs;

	private String outputFile;

	// TODO upgrade mvn dependency to use Keycloak admin 25 to match Keycloak version

	/**
	 * Create and initialize a client to interact with a Keycloak realm.
	 * @param configProperties the properties defining the session
	 * @param environment the environment for the current session
	 * @param batchNumber 
	 * @throws IOException if the output locations cannot be initialized
	 */
	public KeycloakService(Properties configProperties, EnvironmentEnum environment, String batchNumber) throws Exception {
		logger.info("Initializing Keycloak connection against: {}", configProperties.getProperty(CONFIG_PROPERTY_URL));

		this.batchNumber = batchNumber;
		
		realm = configProperties.getProperty(CONFIG_PROPERTY_REALM);
		logger.info("Using Realm: {}", realm);

		// Get the Keycloak client and authenticate.
		Keycloak keycloak = KeycloakBuilder.builder()
				.serverUrl(configProperties.getProperty(CONFIG_PROPERTY_URL))
				.realm(realm)
				.grantType(OAuth2Constants.CLIENT_CREDENTIALS)
				.clientId(configProperties.getProperty(CONFIG_PROPERTY_CLIENT_ID))
				.clientSecret(getClientSecret(environment))				
				.build();

		// Get the realm resources.
		realmResource = keycloak.realm(realm);

		// If defined, set the keystore format.
		if (StringUtils.isNoneBlank(configProperties.getProperty(CONFIG_PROPERTY_KEYSTORE_FORMAT))) {
			keystoreFormat = configProperties.getProperty(CONFIG_PROPERTY_KEYSTORE_FORMAT);
		};

		fileExtension = determineFileExtension();

		outputLocation = configProperties.getProperty(CONFIG_PROPERTY_OUTPUT_LOCATION) + "\\" + batchNumber;

		logger.info("Keycloak connection initialized.");
	}

	/**
	 * Load the client secret from an environment variable determined by the environment.
	 * @param environment the environment
	 * @return the cliet secret
	 */
	private static String getClientSecret(EnvironmentEnum environment) {
		return System.getenv(environment.getClientSecret());
	}

	/**
	 * Create multiple Keycloak clients and return their credentials.
	 * @param configProperties the properties defining the session
	 * @param numberOfClients the number of clients to create
	 * @param clientStartNumber the ID of the first client
	 * @return a set of credentials for each client
	 */
	public List<ClientCredentials> addClients(Properties configProperties, int numberOfClients, int clientStartNumber) {
		logger.info("Begin addClients...");

		// Load the client resource from Keycloak.
		ClientsResource clientsResource = realmResource.clients();

		// Declare the list if client credentials.
		List<ClientCredentials> clientCredentials = new ArrayList<>();

		// Load the list of scopes that each client will be assigned.
		List<String> clientScopes = retrieveClientScopes(configProperties);

		try {
			for (int i = 0; i < numberOfClients; i++) {
				ClientCredentials clientCredential = addClient(clientsResource, clientScopes, clientStartNumber + i);

				if (clientCredentials != null) {
					clientCredentials.add(clientCredential);
				}
			}

			writeCsvFromBean(clientCredentials);
		} catch (Exception e) {
			logger.error("Error during batch run. Stopping processing. All created clients will be removed. Please fix error and retry. Error was: {}", e.getMessage());
			cleanUp(clientCredentials);
		}

		logger.info("End addClients.");
		return clientCredentials;
	}

	/**
	 * Create a single Keycloak client and return its credentials.
	 * @param clientsResource the authenticated and authorized Keycloak client resource
	 * @param scopes the list of scopes the client will be assigned
	 * @param clientIteration the numeric ID of the client
	 * @return the new client's credentials
	 */
	public ClientCredentials addClient(ClientsResource clientsResource, List<String> scopes, int clientIteration) throws Exception {
		logger.info("Begin addingClient...");

		String clientSuffix = generateClientIDSuffix(clientIteration);

		// Generate the full client ID and client name.
		String clientId = CLIENT_ID_BASE + clientSuffix;
		String clientName = CLIENT_NAME_BASE + clientSuffix;
		logger.info("Creating: {} : {}", clientId, clientName);

		// Initialize the Keycloak client representation.
		ClientRepresentation cr = new ClientRepresentation();
		cr.setClientId(clientId);
		cr.setName(clientName);
		cr.setDescription(CLIENT_DESCRIPTION);

		// Configure the default parameters of the client representation.
		populateDefaultsClientRepresentation(cr, scopes);

		// Throw an error if the client already exists.
		if (existsClient(clientId, clientsResource)) {
			logger.error("Client {} already exists", clientId);
			throw new Exception(String.format("Client [%s] already exists", clientId));
		}

		// Create the client and store the response.
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

		KeyStoreConfig keyStoreConfig = createKeyStoreConfig(clientId);

		KeyStore keyStore = uploadKeyAndCertificate(clientId, keyStoreConfig.getKeyPassword(), clientResource);

		X509Certificate x509Certificate = (X509Certificate) keyStore.getCertificate(clientId);
		ClientCredentials cc = createClientCredentials(clientId, keyStoreConfig, x509Certificate.getNotBefore().toString(), x509Certificate.getNotAfter().toString());
		logger.info(cc.toString());

		return cc;
	}

	// Generate the client ID suffix based on the numeric ID.
	//TODO refactor to util
	public String generateClientIDSuffix(int clientIteration) {
		String clientSuffix = StringUtils.leftPad(Integer.toString(clientIteration), 8, "0");
		logger.info("Using clientSuffix: {}", clientSuffix);
		return clientSuffix;
	}

	private void populateDefaultsClientRepresentation(ClientRepresentation cr, List<String> scopes) {
		cr.setEnabled(true);

		cr.setPublicClient(false); // Client authentication
		cr.setAuthorizationServicesEnabled(false); // Authorization

		cr.setStandardFlowEnabled(false); // Standard flow
		cr.setImplicitFlowEnabled(false); // Implicit flow
		cr.setDirectAccessGrantsEnabled(false); // Direct access grants
		cr.setServiceAccountsEnabled(true); // Service accounts roles

		cr.setFrontchannelLogout(false);

		cr.setClientAuthenticatorType(AUTH_TYPE_CLIENT_JWT); // Client Authenticator

		// Configure client scopes by setting default scopes. This removes any
		// predetermined defaults by overwriting them with those in the provided
		// list.
		cr.setDefaultClientScopes(scopes);

		/* Add an Audience mapper */
		cr.setProtocolMappers(createAudienceProtocolMapper());
	}

	private ClientCredentials createClientCredentials(String clientId, KeyStoreConfig keyStoreConfig, String validFromDate, String expiryDate) {
		ClientCredentials cc = new ClientCredentials();
		cc.setClientId(clientId);
		cc.setCertFilename(keyStoreConfig.getKeyAlias() + fileExtension);
		cc.setCertAlias(keyStoreConfig.getKeyAlias());
		cc.setKeyPassword(keyStoreConfig.getKeyPassword());
		cc.setStorePassword(keyStoreConfig.getStorePassword());
		cc.setValidFromDate(validFromDate);
		cc.setExpirtyDate(expiryDate);
		return cc;
	}

    public KeyStore uploadKeyAndCertificate(String clientId, String keystorePassword, ClientResource clientResource) throws Exception {

//    	Add the realm cert for the realm e.g. v2_pos Certificate from https://common-logon-dev.hlth.gov.bc.ca/auth/admin/master/console/#/v2_pos/realm-settings/keys
		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
//		X509Certificate realmCert = (X509Certificate)certificateFactory.generateCertificate(new FileInputStream(new File("C:\\Users\\dave.p.barrett\\Documents\\Projects\\PPM\\Documents\\BulkClientGeneration\\SampleCerts\\realm\\fromKeycloakGeneratedCert\\moh_applications.cer")));
		X509Certificate realmCert = (X509Certificate)certificateFactory.generateCertificate(new FileInputStream(new File("C:\\Users\\dave.p.barrett\\Documents\\Projects\\PPM\\Documents\\BulkClientGeneration\\SampleCerts\\realm\\fromKeycloakGeneratedCert\\moh_applications.cer")));
        
		//Generate a keypair
    	KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        X509Certificate[] certChain = new X509Certificate[1];
        //Generate an x509 certificate using the keypair (required for creating java key stores)
        X509Certificate x509Certificate = generateX509(kp, 1, clientId);
		certChain[0] = x509Certificate;

        //Generate a java key store
        KeyStore keyStore = KeyStore.getInstance(PKCS12.toString());
        char[] password = keystorePassword.toCharArray();
        keyStore.load(null, password);
        
        //Add the entries
        keyStore.setCertificateEntry(MOH_APPLICATIONS, realmCert);
        keyStore.setKeyEntry(clientId, kp.getPrivate(), password, certChain);
        
        StringWriter sw = new StringWriter();
        try (JcaPEMWriter pw = new JcaPEMWriter(sw)) {
            pw.writeObject(x509Certificate);
        }
        // Store the public key in cert format
        String x509CertificatePath = outputLocationX509Certs + clientId + FILE_EXTENSION_CRT;
        FileOutputStream certFos = new FileOutputStream(x509CertificatePath, false);
        certFos.write(sw.toString().getBytes(StandardCharsets.UTF_8));
        certFos.close();
        
        //Upload the newly created public cert to the keycloak client's as its JWT signing public key, the private key will be sent to the client in the pfx file created later
        
		ClientAttributeCertificateResource certificateResource = clientResource.getCertficateResource(JWT_CREDENTIAL);

		MultipartFormDataOutput keyCertForm = new MultipartFormDataOutput();

		keyCertForm.addFormData("keystoreFormat", PKCS12.toString(), MediaType.TEXT_PLAIN_TYPE);
		keyCertForm.addFormData("keyAlias", clientId, MediaType.TEXT_PLAIN_TYPE);
		keyCertForm.addFormData("keyPassword", keystorePassword, MediaType.TEXT_PLAIN_TYPE);
		keyCertForm.addFormData("storePassword", keystorePassword, MediaType.TEXT_PLAIN_TYPE);

		FileInputStream fs = new FileInputStream(x509CertificatePath);
		byte[] x509CertificateContent = fs.readAllBytes();
		fs.close();

		MultipartFormDataOutput form = new MultipartFormDataOutput();
		form.addFormData("keystoreFormat", "Certificate PEM", MediaType.TEXT_PLAIN_TYPE);
		form.addFormData("file", x509CertificateContent, MediaType.APPLICATION_OCTET_STREAM_TYPE);
		CertificateRepresentation uploadedJksCertificate = certificateResource.uploadJksCertificate(form);
		logger.debug("Certificate: {}", uploadedJksCertificate.getCertificate());
		logger.debug("privateKey not included: {}", uploadedJksCertificate.getPrivateKey());

		File f = new File(outputLocationCerts + clientId + fileExtension);
		FileOutputStream fos = new FileOutputStream(f);
		keyStore.store(fos, keystorePassword.toCharArray());

		return keyStore;
	}

	private X509Certificate generateX509(KeyPair keyPair, int certExpiryYears, String clientId) throws OperatorCreationException, CertificateException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		return generateX509(keyPair.getPublic(), keyPair.getPrivate(), certExpiryYears, clientId);
	}

	private X509Certificate generateX509(PublicKey publicKey, PrivateKey privateKey, int certExpiryYears, String clientId) throws OperatorCreationException, CertificateException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		// Setup effective and expiry dates.
		// Bouncy Castle currently only works with java.util.Date but it's still
		// nicer to create them with java.time.LocalDate.
		LocalDate effectiveLocalDate = LocalDate.now();
		LocalDate expiryLocalDate = effectiveLocalDate.plusYears(certExpiryYears);

		Date effectiveDate = java.sql.Date.valueOf(effectiveLocalDate);
		Date expiryDate = java.sql.Date.valueOf(expiryLocalDate);

		// Random for the cert serial.
		SecureRandom random = new SecureRandom();

		// Set X509 initialization properties.
		X500Name issuer = new X500Name("CN=" + clientId);
		BigInteger serial = new BigInteger(160, random);
		Time notBefore = new Time(effectiveDate);
		Time notAfter = new Time(expiryDate);
		X500Name subject = new X500Name("CN=" + clientId);
		SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());

		// Create cert builder.
		X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(issuer, serial, notBefore, notAfter, subject, publicKeyInfo);

		// Create cert signer using private key.
		ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(privateKey);

		// Create the X509 certificate.
		X509CertificateHolder certHolder = certBuilder.build(signer);

		// Extract X509 cert from custom Bouncy Castle wrapper.
		X509Certificate cert = new JcaX509CertificateConverter().setProvider(new BouncyCastleProvider()).getCertificate(certHolder);

		cert.verify(publicKey);

		return cert;
	}

	private KeyStoreConfig createKeyStoreConfig(String clientId) {
		KeyStoreConfig keyStoreConfig = new KeyStoreConfig();

		String password = generatePassword();

		logger.info("ClienID: {} : Password {}", clientId, password);

		keyStoreConfig.setRealmCertificate(true);
		keyStoreConfig.setRealmAlias(realm);
		keyStoreConfig.setKeyAlias(clientId);
		keyStoreConfig.setKeyPassword(password);
		keyStoreConfig.setStorePassword(password);
		keyStoreConfig.setFormat(keystoreFormat);
		return keyStoreConfig;
	}

	private String generatePassword() {
		RandomStringGenerator generator = new RandomStringGenerator.Builder()
				.selectFrom(PASSWORD_CHARACTERS.toCharArray())
				.build();
		String password = generator.generate(16);
		return password;
	}

	/*
	 * Creates an Audience Mapper
	 */
	private List<ProtocolMapperRepresentation> createAudienceProtocolMapper() {

		// Sample Mapper config from existing client with Audience mapper.
		// Protocol: openid-connect
		// ProtocolMapper: oidc-audience-mapper
		// ConsentText: null
		// ID: 590d6374-6f6b-4870-b1ac-4d0856262eb3
		// Name: Pharmanet Audience
		// Key: id.token.claim; Value: false
		// Key: access.token.claim; Value: true
		// Key: included.custom.audience; Value: pharmanet

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

	private void writeCsvFromBean(List<ClientCredentials> clientCredentials) throws Exception {

		Path path = Paths.get(outputFile);
		try (Writer writer = new FileWriter(path.toString())) {

			CustomColumnPositionStrategy<ClientCredentials> mappingStrategy = new CustomColumnPositionStrategy<ClientCredentials>();
	        mappingStrategy.setType(ClientCredentials.class);

	        StatefulBeanToCsv<ClientCredentials> sbc = new StatefulBeanToCsvBuilder<ClientCredentials>(writer)
					.withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
					.withSeparator(CSVWriter.DEFAULT_SEPARATOR)
					.withMappingStrategy(mappingStrategy)
					.build();

			sbc.write(clientCredentials);
		} catch (IOException | CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
			logger.error("Error writing client details", e.getMessage());
			throw new Exception("Error writing file");
		}
	}

	/**
	 * Remove all items produced for these clients. Including
	 * 	Keycloak client entry
	 * 	all output files saved to drive for this batch
	 * @param clientCredentials
	 */
	public void cleanUp(List<ClientCredentials> clientCredentials) {
		processClientsCleanUp(clientCredentials);
		deleteArtifacts();
	}
	
	public void processClientsCleanUp(List<ClientCredentials> clientCredentials) {

		ClientsResource clientsResource = realmResource.clients();

		clientCredentials.forEach(cc -> {
			processClientCleanUp(cc.getClientId(), clientsResource);
		});
	}

	private void processClientCleanUp(String clientId, ClientsResource clientsResource) {
		ClientRepresentation clientRepresentation = retrieveClient(clientsResource, clientId);
		if (clientRepresentation != null) {
			printClientDetails(clientRepresentation);
			deleteClient(clientsResource, clientRepresentation.getId());
		}
	}

	private void deleteArtifacts() {
		logger.info("Deleting {}", outputLocation);
		
		try {
			File batchLocation = new File(outputLocation);			
			FileUtils.deleteDirectory(batchLocation);
		} catch (IOException e) {
			logger.info("Failed to delete {}", outputLocation);
		}
		logger.info("Deleted {}", outputLocation);
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

	/**
	 * Initialize the output files and directories for this Keycloak session.
	 * @param configPropertiesthe properties defining the session's output locations
	 * @throws IOException if the output directory cannot be created or if the output file already exists
	 */
	public void initOutput() throws Exception {
		logger.debug("Initializing file location.");

		// Build the output paths.
		outputLocationCerts = outputLocation + "\\certs\\";
		outputLocationX509Certs = outputLocation + "\\certs\\x509\\";
		outputFile = outputLocation + "\\client_information_" + batchNumber + ".csv";

		File outputLocationX509CertsDir = new File(outputLocationX509Certs);

		// Create the certificate output directory if it doesn't already exist.
		if (!outputLocationX509CertsDir.exists()) {
			logger.debug("File location directory [{}] did not exist so it will be created.", outputLocationX509Certs);
			boolean dirCreated = outputLocationX509CertsDir.mkdirs();

			// Throw an exception if the directory could not be created.
			if (!dirCreated) {
				logger.error("Failed to initialize output dir: {}", outputLocationX509CertsDir.getAbsolutePath());
				throw new IOException("Failed to initialize output dir");
			}
		}

		File outputFileFile = new File(outputFile);

		// Verify that the output file does not already exist.
		if (outputFileFile.exists()) {
			throw new IOException(String.format("Output file %s already exists. Assign new batch number for this run.", outputFile));
		}

		logger.info("Output file is: {}. Certs are in: {}", outputFile, outputLocationCerts);
	}

	private String determineFileExtension() {
		switch (KeystoreFormat.valueOf(keystoreFormat)) {
			case PKCS12:
				return FILE_EXTENSION_PFX;
			case JKS:
				return FILE_EXTENSION_JKS;
			default:
				return null;
		}
	}

}
