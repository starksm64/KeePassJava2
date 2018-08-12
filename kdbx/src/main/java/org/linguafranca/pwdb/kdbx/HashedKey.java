package org.linguafranca.pwdb.kdbx;

import java.security.MessageDigest;

import org.linguafranca.pwdb.Credentials;
import org.linguafranca.pwdb.security.Encryption;

public class HashedKey implements Credentials {
    private byte[] key;

    HashedKey(byte[] bytes) {
        MessageDigest md = Encryption.getSha256MessageDigestInstance();
        this.key = md.digest(bytes);
    }
    @Override
    public byte[] getKey() {
        return key;
    }
}
