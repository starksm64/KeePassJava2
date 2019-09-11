package com.si.keypass.prefs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import io.jsondb.JsonDBTemplate;

public class PrefsUtils {
    /**
     * The base directory for the Jsondb files
     */
    public static final String JSONDB_PREF_LOCATION = "/Library/Preferences/SIPreferences";

    /**
     * Convert the fully qualified class name into a / separated path.
     * /package-name/classs-simple-name. A class in the default package will
     * have a leading path of /<unnamed>/
     *
     * @param className - the class to create a path for
     * @return the absolute path name form of the class
     */
    public static String pathForClass(String className) {
        int pkgEndIndex = className.lastIndexOf('.');
        String packageName;
        String simpleName;
        if (pkgEndIndex < 0) {
            packageName = "/<unnamed>";
            simpleName = className;
        }
        else {
            packageName = className.substring(0, pkgEndIndex);
            simpleName = className.substring(pkgEndIndex+1);
        }
        String nodePath =  "/" + packageName.replace('.', '/');
        return nodePath + "/" + simpleName;
    }

    /**
     * Create a secured JsonDBTemplate using the default KeyStore as provided by {@link KeystoreUtils}
     * @param clazz - the annotated preferences class
     * @param <T> - the type of the class
     * @return jsondb template for accessing the collection
     */
    public static <T> JsonDBTemplate getPreferences(Class<T> clazz) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        KeyStore keyStore = KeystoreUtils.loadKeyStore();
        return getPreferences(clazz, keyStore, "keyprotection");
    }
    /**
     * Create a secured JsonDBTemplate using the default KeyStore as provided by {@link KeystoreUtils}
     * @param clazz - the annotated preferences class
     * @param cipherAlias - alias of key in keystore used by cipher
     * @param <T> - the type of the class
     * @return jsondb template for accessing the collection
     */
    public static <T> JsonDBTemplate getPreferences(Class<T> clazz, String cipherAlias) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        KeyStore keyStore = KeystoreUtils.loadKeyStore();
        return getPreferences(clazz, keyStore, cipherAlias);
    }

    /**
     * Create a secured JsonDBTemplate using the givene KeyStore
     * @param clazz - the annotated preferences class
     * @param cipherAlias - alias of key in keystore used by cipher
     * @param <T> - the type of the class
     * @return jsondb template for accessing the collection
     */
    public static <T> JsonDBTemplate getPreferences(Class<T> clazz, KeyStore keyStore, String cipherAlias) throws IOException {
        String userHome = System.getProperty("user.home");
        File jsondbDir = new File(userHome, JSONDB_PREF_LOCATION);
        // Create the root directory if it does not exist
        if (!jsondbDir.exists()) {
            if (!jsondbDir.mkdirs()) {
                throw new FileNotFoundException("Failed to find/create: "+JSONDB_PREF_LOCATION);
            }
        }

        AESCTRCipher cipher = new AESCTRCipher(keyStore, cipherAlias);
        JsonDBTemplate jsonDBTemplate = new JsonDBTemplate(jsondbDir.getAbsolutePath(), clazz.getPackage().getName(), cipher);
        return jsonDBTemplate;
    }

}
