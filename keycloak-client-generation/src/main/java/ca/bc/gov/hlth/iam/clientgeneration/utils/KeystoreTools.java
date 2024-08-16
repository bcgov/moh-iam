package ca.bc.gov.hlth.iam.clientgeneration.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

public class KeystoreTools {

    /**
     * Load a key store from a file.
     * @param keystoreFile the certificate file
     * @param password the password to the key store
     * @param keyStoreType the type of the key store
     * @return the resulting {@code KeyStore}
     * @throws GeneralSecurityException if the key store cannot be loaded due to an issue with the system's certificate algorithms or with the key store's certificates
     * @throws IOException if an error occurs while loading the key store from the certificate file
     */
    public static KeyStore loadKeyStore(File keystoreFile, String password, String keyStoreType) throws GeneralSecurityException, IOException {
        // Open the certificate file as an input stream.
        InputStream is = keystoreFile.toURI().toURL().openStream();

        return loadKeyStore(is, password, keyStoreType);
    }

    /**
     * Load a key store from an input stream.
     * @param is the input stream to the certificate source
     * @param password the password to the key store
     * @param keyStoreType the type of the key store
     * @return the resulting {@code KeyStore}
     * @throws GeneralSecurityException if the key store cannot be loaded due to an issue with the system's certificate algorithms or with the key store's certificates
     * @throws IOException if an error occurs while loading the key store from the certificate file
     */
    public static KeyStore loadKeyStore(InputStream is, String password, String keyStoreType) throws GeneralSecurityException, IOException {
        // Initialize the key store.
        KeyStore keystore = KeyStore.getInstance(keyStoreType);

        // Load the keys from the key store.
        keystore.load(is, password.toCharArray());

        return keystore;
    }

}
