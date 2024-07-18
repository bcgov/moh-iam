package ca.bc.gov.hlth.iam.clientgeneration.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.hlth.iam.clientgeneration.service.KeycloakService;

public class KeystoreTools {

	private static final Logger logger = LoggerFactory.getLogger(KeycloakService.class);

	public static KeyStore loadKeyStore(File keystoreFile, String password, String keyStoreType) throws Exception {
        try (InputStream is = keystoreFile.toURI().toURL().openStream()) {
            return loadKeyStore(is, password, keyStoreType);
        } catch (CertificateException ce) {
        	logger.error("Error loading KeyStore.");
        	throw new Exception(ce);
        }
    }

	public static KeyStore loadKeyStore(InputStream is, String password, String keyStoreType)
			throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
		KeyStore keystore = KeyStore.getInstance(keyStoreType);
		keystore.load(is, password.toCharArray());
		return keystore;
	}

}
