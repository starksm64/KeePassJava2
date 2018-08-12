package org.linguafranca.pwdb.kdbx;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import org.linguafranca.pwdb.Credentials;
import org.linguafranca.pwdb.security.Encryption;

/**
 * A combination of keys used with multiple key file values for example
 */
public class CompositeKey implements Credentials {
    private List<Credentials> keys = new ArrayList<>();

    public void addKey(Credentials key) {
        keys.add(key);
    }

    @Override
    public byte[] getKey() {
        MessageDigest md = Encryption.getSha256MessageDigestInstance();
        for(Credentials key : keys) {
            md.update(key.getKey());
        }
        byte[] key = md.digest();
        return key;
    }

    @Override
    public String toString() {
        StringBuilder tmp = new StringBuilder(String.format("CompositeKey:%s\n", keys.size()));
        for(Credentials key : keys) {
            tmp.append("\t"+Helpers.encodeBase64Content(key.getKey())+"\n");
        }
        return tmp.toString();
    }
}
