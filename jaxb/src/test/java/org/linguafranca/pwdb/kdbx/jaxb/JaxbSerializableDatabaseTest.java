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

package org.linguafranca.pwdb.kdbx.jaxb;

import org.junit.Test;
import org.linguafranca.pwdb.Visitor;
import org.linguafranca.pwdb.kdbx.CompositeKey;
import org.linguafranca.pwdb.kdbx.HashedKey;
import org.linguafranca.pwdb.kdbx.Helpers;
import org.linguafranca.pwdb.kdbx.KdbxCreds;
import org.linguafranca.pwdb.kdbx.KdbxHeader;
import org.linguafranca.pwdb.kdbx.KdbxSerializer;
import org.linguafranca.pwdb.kdbx.jaxb.binding.BinaryField;
import org.linguafranca.pwdb.kdbx.jaxb.binding.CustomIcons;
import org.linguafranca.pwdb.kdbx.jaxb.binding.JaxbEntryBinding;
import org.linguafranca.pwdb.kdbx.jaxb.binding.JaxbGroupBinding;
import org.linguafranca.pwdb.kdbx.jaxb.binding.KeePassFile;
import org.linguafranca.pwdb.kdbx.jaxb.binding.StringField;
import org.linguafranca.pwdb.kdbx.jaxb.binding.Times;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

/**
 * @author jo
 */
public class JaxbSerializableDatabaseTest {
    @Test
    public void createEmptyDatabase() throws Exception {
        JaxbDatabase db = JaxbDatabase.createEmptyDatabase();
        db.save(new KdbxCreds.None(), System.out);
    }
    @Test
    public void loadXml() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("test123.kdbx");
        JaxbDatabase database = JaxbDatabase.load(new KdbxCreds("123".getBytes()), inputStream);
        database.visit(new Visitor.Print());
    }

    /**
     * Try loading the SIKeyPass.kdbx into a JaxbSerializableDatabase
     * @throws Exception
     */
    @Test
    public void testReadDBXWithPassAndKeyfile() throws Exception {
        // get an input stream from KDB file
        //String rootDir = "/Users/starksm/Google Drive/Private/";
        String rootDir = "/media/starksm/Samsung USB/";
        // A test dbx with a password of KeyPass.kdbx
        String kdbxFile = "SIKeyPass.kdbx";
        FileInputStream kdbxIS = new FileInputStream(rootDir+kdbxFile);
        // Read the password from /tmp/testLoadDB.pass
        FileReader reader = new FileReader("/tmp/testLoadDB.pass");
        BufferedReader br = new BufferedReader(reader);
        String pass = br.readLine();
        br.close();
        CompositeKey credentials = new CompositeKey();
        KdbxCreds creds = new KdbxCreds(pass.getBytes());
        System.out.printf("KdbxCreds: %s\n", Helpers.encodeBase64Content(creds.getKey()));
        credentials.addKey(creds);
        String[] keyFiles = {rootDir+"myqrcode.png", rootDir+"EveningFullPassort_20171229.jpg", rootDir+"ScottFullPassort_20171229.jpg"};
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
        KdbxHeader kdbxHeader = new KdbxHeader();
        System.out.printf("Header.getCipherUuid: %s\n", kdbxHeader.getCipherUuid());
        System.out.printf("Header.getProtectedStreamAlgorithm: %s\n", kdbxHeader.getProtectedStreamAlgorithm());
        System.out.printf("Header.getVersion: %s\n", kdbxHeader.getVersion());
        InputStream decryptedInputStream = KdbxSerializer.createUnencryptedInputStream(credentials, kdbxHeader, kdbxIS);
        JaxbSerializableDatabase db = new JaxbSerializableDatabase();
        db.setEncryption(kdbxHeader.getStreamEncryptor());
        db.load(decryptedInputStream);
        KeePassFile dbFile = db.getKeePassFile();
        KeePassFile.Root root = dbFile.getRoot();
        List<JaxbGroupBinding> groups = root.getGroup().getGroup();
        StringBuilder output = new StringBuilder();
        output.append("SIKeyPass.kdbx\n");
        toString(root.getGroup(), output, "");
        for(JaxbGroupBinding group : groups) {
            toString(group, output, "  ");
        }
        System.out.println(output.toString());

        // Test the icons
        int rootIconID = root.getGroup().getIconID();
        System.out.printf("rootIconID: %d\n", rootIconID);
        List<CustomIcons.Icon> icons = dbFile.getMeta().getCustomIcons().getIcon();
        for (CustomIcons.Icon icon : icons) {
            ByteArrayInputStream data = new ByteArrayInputStream(icon.getData());
            FileOutputStream tmpIcon = new FileOutputStream("/tmp/icon");
            tmpIcon.write(icon.getData());
            tmpIcon.close();
            BufferedImage image = ImageIO.read(data);
            System.out.printf("%s, %dx%d\n", icon.getUUID(), image.getHeight(), image.getWidth());
        }
    }

    private void toString(JaxbGroupBinding group, StringBuilder sb, String indent) {
        sb.append(indent);
        sb.append(String.format("Group(%s/%s)\n", group.getName(), group.getUUID()));
        sb.append(indent); sb.append("  ");
        sb.append(String.format("Subgroups: %d\n", group.getGroup().size()));
        sb.append(indent); sb.append("  ");
        sb.append(String.format("getIsExpanded: %s\n", group.getIsExpanded()));
        sb.append(indent); sb.append("  ");
        sb.append(String.format("getIconID: %d\n", group.getIconID()));
        sb.append(indent); sb.append("  ");
        if(group.getCustomIconUUID() != null) {
            sb.append(String.format("getCustomIconUUID/base64: %s\n", Helpers.base64FromUuid(group.getCustomIconUUID())));
        }
        sb.append(indent); sb.append("  ");
        UUID uuid = group.getUUID();
        sb.append(String.format("getUUID/base64: %s\n", Helpers.base64FromUuid(uuid)));
        sb.append(indent); sb.append("  ");
        sb.append(String.format("getNotes: %s\n", group.getNotes()));
        sb.append(indent); sb.append("  ");
        sb.append(String.format("getTimes: %s\n", toString(group.getTimes())));
        sb.append(indent); sb.append("  ");
        sb.append("Entries:\n");
        for(JaxbEntryBinding entry : group.getEntry()) {
            toString(entry, sb, indent+"  ");
        }
    }
    private void toString(JaxbEntryBinding entry, StringBuilder sb, String indent) {
        sb.append(indent);sb.append("  ");
        sb.append(String.format("Entry(%s):\n", entry.getUUID()));
        sb.append(indent); sb.append("  ");
        sb.append(String.format("getOverrideURL: %s\n", entry.getOverrideURL()));
        sb.append(indent); sb.append("  ");
        sb.append(String.format("getCustomIconUUID: %s\n", entry.getCustomIconUUID()));
        sb.append(indent); sb.append("  ");
        sb.append(String.format("getTags: %s\n", entry.getTags()));
        sb.append(indent); sb.append("  ");
        sb.append(String.format("getTimes: %s\n", toString(entry.getTimes())));
        sb.append(indent); sb.append("  ");
        sb.append(String.format("Strings:\n"));
        for(StringField field : entry.getString()) {
            sb.append(indent); sb.append(String.format("    %s/%s=%s\n", field.getKey(), field.getValue().getProtected(), field.getValue().getValue()));
        }
        sb.append(indent); sb.append("  ");
        sb.append(String.format("Binaries:\n"));
        for(BinaryField field : entry.getBinary()) {
            sb.append(indent); sb.append(String.format("    %s=%s\n", field.getKey(), field.getValue()));
        }

    }
    private String toString(Times times) {
        return String.format("lastModificationTime: %s, creationTime: %s, lastAccessTime: %s, locationChanged: %s, expiryTime: %s, expires: %s, usageCount: %d",
                             times.getLastModificationTime(), times.getCreationTime(), times.getLastAccessTime(), times.getLocationChanged(),
                             times.getExpiryTime(), times.getExpires(), times.getUsageCount());
    }
}