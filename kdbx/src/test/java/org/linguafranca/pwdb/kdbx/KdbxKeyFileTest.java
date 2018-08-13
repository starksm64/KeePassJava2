/*
 * Copyright 2015 Jo Rabin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.linguafranca.pwdb.kdbx;

import org.junit.Test;
import org.linguafranca.pwdb.kdbx.KdbxSerializer;
import org.linguafranca.pwdb.Credentials;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author jo
 */
public class KdbxKeyFileTest {

    @Test
    public void testLoad() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("KeyFileDatabase.key");
        byte[] key = KdbxKeyFile.load(inputStream);
        assertNotNull(key);
        assertEquals(32, key.length);
    }

    @Test
    public void testReadDBX() throws Exception {
        // get an input stream from KDB file
        // A test dbx with a password of KeyPass.kdbx
        String kdbxFile = "/Users/starksm/private/Banking/KeyPass.kdbx";
        FileInputStream kdbxIS = new FileInputStream(kdbxFile);
        // Read the password from /tmp/testLoadDB.pass
        FileReader reader = new FileReader("/tmp/testLoadDB.pass");
        BufferedReader br = new BufferedReader(reader);
        String pass = br.readLine();
        br.close();
        Credentials credentials = new KdbxCreds(pass.getBytes());
        InputStream decryptedInputStream = KdbxSerializer.createUnencryptedInputStream(credentials, new KdbxHeader(), kdbxIS);
        byte[] buffer = new byte[1024];
        while ( decryptedInputStream.available() > 0) {
            int read = decryptedInputStream.read(buffer);
            if (read == -1) break;
            System.out.write(buffer, 0, read);
        }
    }
    @Test
    public void testReadDBXWithPassAndKeyfile() throws Exception {
        // get an input stream from KDB file
        String root = "/Users/starksm/Google Drive/Private/";
        // A test dbx with a password of KeyPass.kdbx
        String kdbxFile = "SIKeyPass.kdbx";
        FileInputStream kdbxIS = new FileInputStream(root+kdbxFile);
        // Read the password from /tmp/testLoadDB.pass
        FileReader reader = new FileReader("/tmp/testLoadDB.pass");
        BufferedReader br = new BufferedReader(reader);
        String pass = br.readLine();
        br.close();
        CompositeKey credentials = new CompositeKey();
        KdbxCreds creds = new KdbxCreds(pass.getBytes());
        System.out.printf("KdbxCreds: %s\n", Helpers.encodeBase64Content(creds.getKey()));
        credentials.addKey(creds);
        String[] keyFiles = {root+"myqrcode.png", root+"EveningFullPassort_20171229.jpg", root+"ScottFullPassort_20171229.jpg"};
        byte[] tmp = new byte[4096];
        for(String keyFile : keyFiles) {
            System.out.printf("Reading: %s\n", keyFile);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try(FileInputStream fis = new FileInputStream(keyFile)) {
                int read = fis.read(tmp);
                do {
                    baos.write(tmp, 0, read);
                    read = fis.read(tmp);
                } while (read > 0);
            }
            baos.close();
            byte[] key = baos.toByteArray();
            System.out.printf("\tbytes: %d\n", key.length);
            HashedKey hkey = new HashedKey(key);
            System.out.printf("\t%s\n", Helpers.encodeBase64Content(hkey.getKey()));
            credentials.addKey(hkey);
        }
        System.out.printf(credentials.toString());
        InputStream decryptedInputStream = KdbxSerializer.createUnencryptedInputStream(credentials, new KdbxHeader(), kdbxIS);
        byte[] buffer = new byte[1024];
        FileOutputStream fos = new FileOutputStream("/tmp/test.kdbx");
        while ( decryptedInputStream.available() > 0) {
            int read = decryptedInputStream.read(buffer);
            if (read == -1) break;
            fos.write(buffer, 0, read);
        }
        fos.close();
        System.out.printf("Wrote to /tmp/test.kdbx\n");
    }
    @Test
    public void loadSIDBHeader() throws IOException {
        InputStream inputStream = new FileInputStream(new File("/Users/starksm/Google Drive/Private/SIKeyPass.kdbx"));
        KdbxHeader header = KdbxSerializer.readOuterHeader(inputStream, new KdbxHeader());
        System.out.println("Version " + header.getVersion());
        System.out.println("getCipherUuid " + header.getCipherUuid());
        System.out.println("getCompressionFlags " + header.getCompressionFlags());
        System.out.println("getTransformRounds " + header.getTransformRounds());
        System.out.println("getProtectedStreamAlgorithm " + header.getProtectedStreamAlgorithm());
        System.out.println("getStreamEncryptor " + header.getStreamEncryptor());
        System.out.println("Binaries.size: " + header.getBinaries().size());

    }
    /*
    Test for empty password
     */
    @Test
    public void testEmptyPasswordCreds() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("EmptyPassword.kdbx");
        Credentials credentials = new KdbxCreds(new byte[0]);
        InputStream decryptedInputStream = KdbxSerializer.createUnencryptedInputStream(credentials, new KdbxHeader(), inputStream);
        byte[] buffer = new byte[1024];
        while ( decryptedInputStream.available() > 0) {
            int read = decryptedInputStream.read(buffer);
            if (read == -1) break;
            System.out.write(buffer, 0, read);
        }
    }


    /**
     Test for empty password with key
     */
    @Test
    public void testEmptyPasswordKeyCreds() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("EmptyPasswordWithKey.kdbx");
        InputStream inputStreamKeyFile = getClass().getClassLoader().getResourceAsStream("EmptyPasswordWithKey.key");
        Credentials credentials = new KdbxCreds(new byte[0], inputStreamKeyFile);
        InputStream decryptedInputStream = KdbxSerializer.createUnencryptedInputStream(credentials, new KdbxHeader(), inputStream);
        byte[] buffer = new byte[1024];
        while ( decryptedInputStream.available() > 0) {
            int read = decryptedInputStream.read(buffer);
            if (read == -1) break;
            System.out.write(buffer, 0, read);
        }
    }

    /**
     Test for no master password
     */
    @Test
    public void testNoPasswordKeyCreds() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("NoPasswordWithKey.kdbx");
        InputStream inputStreamKeyFile = getClass().getClassLoader().getResourceAsStream("NoPasswordWithKey.key");
        Credentials credentials = new KdbxCreds(inputStreamKeyFile);
        InputStream decryptedInputStream = KdbxSerializer.createUnencryptedInputStream(credentials, new KdbxHeader(), inputStream);
        byte[] buffer = new byte[1024];
        while ( decryptedInputStream.available() > 0) {
            int read = decryptedInputStream.read(buffer);
            if (read == -1) break;
            System.out.write(buffer, 0, read);
        }
    }

    /*
    Test for empty password
     */
    @Test
    public void testEmptyPassword() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("EmptyPassword.kdbx");
        Credentials credentials = new KdbxCreds(new byte[0]);
        InputStream decryptedInputStream = KdbxSerializer.createUnencryptedInputStream(credentials, new KdbxHeader(), inputStream);
        byte[] buffer = new byte[1024];
        while ( decryptedInputStream.available() > 0) {
            int read = decryptedInputStream.read(buffer);
            if (read == -1) break;
            System.out.write(buffer, 0, read);
        }
    }
}