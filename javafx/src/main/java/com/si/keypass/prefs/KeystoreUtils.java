package com.si.keypass.prefs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class KeystoreUtils {
    public static String KEYSTORE_PATH_PROPERTY = "com.si.keypass.prefs.keyStore";
    private static String KEYSTORE_DEFAULT_PATH = "HOME/Applications/keys.jks";

    static String getKeystoreDefaultPath() {
        String home = System.getenv("HOME");
        String path = KEYSTORE_DEFAULT_PATH.replace("HOME", home);
        return path;
    }

    /**
     * Load a default keystore.
     * @return the KeyStore JKS type instance
     * @throws KeyStoreException
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     */
    public static KeyStore loadKeyStore() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        return loadKeyStore(null, "keyprotection".toCharArray());
    }

    /**
     * Load a keystore from the path or resource given by the KEYSTORE_PATH_PROPERTY. If there
     * is no such property, a default of "/keys.jks" is used.
     * @param storePass - the password for accessing the keystore
     * @return the KeyStore JKS type instance
     * @throws KeyStoreException
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     */
    public static KeyStore loadKeyStore(char[] storePass) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        return loadKeyStore(null, storePass);
    }
    /**
     * Load a keystore from the path or resource given by the storePath.
     * @param storePath - a filesystem or resource path to keystore. If null, KEYSTORE_PATH_PROPERTY or
     *                  a default of "/keys.jks" is used.
     * @param storePass - the password for accessing the keystore
     * @return the KeyStore JKS type instance
     * @throws KeyStoreException
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     */
    public static KeyStore loadKeyStore(String storePath, char[] storePass) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        String defaultPath = getKeystoreDefaultPath();
        if(storePath == null) {
            storePath = System.getProperty(KEYSTORE_PATH_PROPERTY);
            if(storePath == null) {
                File test = new File(defaultPath);
                if(test.canRead())
                    storePath = defaultPath;
                else
                    storePath = "/keys.jks";
            }
        }
        InputStream kis;
        File path = new File(storePath);
        if(path.canRead()) {
            kis = new FileInputStream(path);
        } else {
            kis = KeystoreUtils.class.getResourceAsStream(storePath);
        }
        if(kis == null) {
            throw new FileNotFoundException(String.format("No keystore found amongst: %s, %s", defaultPath, storePath));
        }
        keyStore.load(kis, storePass);
        kis.close();
        return keyStore;
    }

}

