package com.si.keypass.prefs;


import java.security.KeyStore;

import io.jsondb.JsonDBException;
import io.jsondb.crypto.ICipher;

/**
 * An implementation of {@link ICipher} that uses AES/CTR/NO via the {@link AESUtils} class.
 *
 * @see AESUtils#decryptText(KeyStore, String, char[], String)
 * @see AESUtils#encryptText(KeyStore, String, char[], String)
 */
public class AESCTRCipher implements ICipher {
    private KeyStore keyStore;
    private String alias;

    /**
     * Create a cipher using the given keystore and a default alias to obtain the key material
     * @param keyStore - a java keystore with the private keys to use
     */
    public AESCTRCipher(KeyStore keyStore) {
        this(keyStore, "keyprotection");
    }

    /**
     * Create a cipher using the given keystore and alias to obtain the key material
     * @param keyStore - a java keystore with the private keys to use
     * @param alias - the alias of the private key to use
     */
    public AESCTRCipher(KeyStore keyStore, String alias) {
        this.keyStore = keyStore;
        this.alias = alias;
    }

    @Override
    public String encrypt(String plainText) {
        try {
            return AESUtils.encryptText(keyStore, alias, "keyprotection".toCharArray(), plainText);
        } catch (Exception e) {
            throw new JsonDBException("AESCTRCipher failed to encrypt text", e);
        }
    }

    @Override
    public String decrypt(String cipherText) {
        try {
            return AESUtils.decryptText(keyStore, alias, "keyprotection".toCharArray(), cipherText);
        } catch (Exception e) {
            throw new JsonDBException("AESCTRCipher failed to decrypt text", e);
        }
    }
}
