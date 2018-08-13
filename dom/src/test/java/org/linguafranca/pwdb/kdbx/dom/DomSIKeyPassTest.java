package org.linguafranca.pwdb.kdbx.dom;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.List;

import org.junit.Test;
import org.linguafranca.pwdb.kdbx.CompositeKey;
import org.linguafranca.pwdb.kdbx.HashedKey;
import org.linguafranca.pwdb.kdbx.Helpers;
import org.linguafranca.pwdb.kdbx.KdbxCreds;

/**
 * Test of loading the SIKeyPass.kdbx into a DomSerializableDatabase
 */
public class DomSIKeyPassTest {
    @Test
    public void loadDB() throws Exception {
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

        // open database. DomDatabaseWrapper is so-called, since it wraps
        // a W3C DOM, populated from the KeePass XML, and presents it
        // through a org.linguafranca.keepass.Database interface.
        DomDatabaseWrapper db = DomDatabaseWrapper.load(credentials, kdbxIS);
        System.out.printf("db.name: %s\n", db.getName());
        System.out.printf("getEntriesCount: %d\n", db.getRootGroup().getEntriesCount());
        System.out.printf("getGroupsCount: %d\n", db.getRootGroup().getGroupsCount());
        List<DomGroupWrapper> groups = db.getRootGroup().getGroups();
        for (DomGroupWrapper group : groups) {
            System.out.printf("Group(%s)\n", group.getName());
            System.out.printf("\tisRecycleBin: %s\n", group.isRecycleBin());
            System.out.printf("\tisRootGroup: %s\n", group.isRootGroup());
            System.out.printf("\tgetGroupsCount: %d\n", group.getGroupsCount());
            System.out.printf("\tgetEntriesCount: %d\n", group.getEntriesCount());
            List<DomEntryWrapper> entries =  group.getEntries();
            for (DomEntryWrapper entry : entries) {
                System.out.printf("\tTitle:%s\n", entry.getTitle());
                System.out.printf("\t\tgetBinaryPropertyNames: %s\n", entry.getBinaryPropertyNames());
                System.out.printf("\t\tgetPropertyNames: %s\n", entry.getPropertyNames());
                System.out.printf("\t\tUserName: %s\n", entry.getProperty("UserName"));
                System.out.printf("\t\tPassword: %s\n", entry.getProperty("Password"));

            }
        }
    }

    void displayEntry(DomEntryWrapper entry) {

    }
}
