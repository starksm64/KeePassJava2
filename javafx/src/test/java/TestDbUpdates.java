import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.si.keypass.DBUtils;
import com.si.keypass.KeePassEntry;
import javafx.scene.image.ImageView;
import org.apache.commons.codec.binary.Base64;
import org.junit.Assert;
import org.junit.Test;
import org.linguafranca.pwdb.Credentials;
import org.linguafranca.pwdb.kdbx.KdbxHeader;
import org.linguafranca.pwdb.kdbx.KdbxSerializer;
import org.linguafranca.pwdb.kdbx.StreamEncryptor;
import org.linguafranca.pwdb.kdbx.jaxb.JaxbDatabase;
import org.linguafranca.pwdb.kdbx.jaxb.JaxbEntry;
import org.linguafranca.pwdb.kdbx.jaxb.JaxbGroup;
import org.linguafranca.pwdb.kdbx.jaxb.binding.JaxbEntryBinding;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import static org.linguafranca.pwdb.kdbx.KdbxStreamFormat.Version.KDBX31;

/**
 * Tests of modifying and saving/exporting a kdbx file
 */
public class TestDbUpdates {
    // the following tests make use of files on a usb thumb drive
    static String USB_PATH = "/media/starksm/Samsung USB";
    @Test
    public void testSaveAndExportOct5Db() throws IOException {
        saveAndExport(USB_PATH + "/SIKeyPass.kdbx");
    }
    @Test
    public void testSaveAndExportHWupdate() throws IOException {
        saveAndExport("/home/starksm/tmp/HWupdate");
    }
    private void saveAndExport(String dbPath) throws IOException {
        // usb kdbx file saved on Oct 5 2019
        File kdbxFile = new File(dbPath);
        Assert.assertTrue(kdbxFile.getAbsolutePath(), kdbxFile.canRead());
        // Needs a default.map on test system
        String[] pathInfo = DBUtils.loadDefault();
        String pass = pathInfo[1];
        String keyFileInfo = pathInfo[2];
        JaxbDatabase testDb = DBUtils.loadDB(kdbxFile, pass, keyFileInfo);

        Assert.assertEquals("SI.com MasterDB", testDb.getName());
        Assert.assertEquals("The master database for the SI empire", testDb.getDescription());
        JaxbGroup root = testDb.getRootGroup();
        Assert.assertEquals("Root", root.getName());
        // Export the db

        Credentials credentials = DBUtils.createCredentials(pass, keyFileInfo);
        String dbName = kdbxFile.getName();
        DBUtils.saveDb(testDb, new File("/tmp/"+dbName+".kdbx"), credentials);
        DBUtils.exportDb(testDb, new File("/tmp/"+dbName+".json"));

        // Reload the saved db and export it
        File tmp = new File("/tmp/"+dbName+".kdbx");
        JaxbDatabase db2 = DBUtils.loadDB(tmp, pass, keyFileInfo);
        DBUtils.exportDb(db2, new File("/tmp/"+dbName+"2.json"));
    }

    @Test
    public void testUpdateSaveExport() throws Exception {
        // usb kdbx file saved on Oct 5 2019
        File kdbxFile = new File(USB_PATH, "SIKeyPass.kdbx");
        Assert.assertTrue(kdbxFile.getAbsolutePath(), kdbxFile.canRead());
        // Needs a default.map on test system
        String[] pathInfo = DBUtils.loadDefault();
        String pass = pathInfo[1];
        String keyFileInfo = pathInfo[2];
        JaxbDatabase oct5Db = DBUtils.loadDB(kdbxFile, pass, keyFileInfo);

        Assert.assertEquals("SI.com MasterDB", oct5Db.getName());
        Assert.assertEquals("The master database for the SI empire", oct5Db.getDescription());
        JaxbGroup root = oct5Db.getRootGroup();
        Assert.assertEquals("Root", root.getName());
        // Updated the db
        // Write out the template json
        JaxbEntry jaxbEntry = oct5Db.newEntry();
        oct5Db.getRootGroup().addEntry(jaxbEntry);
        Map<UUID, ImageView> iconsMap = new HashMap<>();
        KeePassEntry selectedEntry = new KeePassEntry(jaxbEntry.getParent(), jaxbEntry, iconsMap);
        selectedEntry.setTitle("NewEntryTitle");
        selectedEntry.setPassword("testUpdateSaveExport-pass");
        selectedEntry.setUsername("testUpdateSaveExport-user");
        selectedEntry.setNotes("testUpdateSaveExport-notes");
        selectedEntry.setURL("testUpdateSaveExport-url");
        selectedEntry.addAttribute("SSN", "123-80-4567");

        JaxbEntryBinding entryBinding = selectedEntry.getDelegate();
        JAXBContext jc = JAXBContext.newInstance(JaxbEntryBinding.class);
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        StringWriter writer = new StringWriter();
        marshaller.marshal(entryBinding, writer);
        String json = writer.toString();
        json = json.replace("NewEntryTitle", "testUpdateSaveExport");
        // Get the updated text and unmarshall it
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        JaxbEntryBinding updatedBinding = (JaxbEntryBinding) unmarshaller.unmarshal(new StringReader(json));
        selectedEntry.updateDelegate(updatedBinding);

        Credentials credentials = DBUtils.createCredentials(pass, keyFileInfo);
        DBUtils.saveDb(oct5Db, new File("/tmp/testUpdateSaveExport.kdbx"), credentials);
        DBUtils.exportDb(oct5Db, new File("/tmp/testUpdateSaveExport.json"));

        // Reload the saved db and export it
        File tmp = new File("/tmp/testUpdateSaveExport.kdbx");
        JaxbDatabase db2 = DBUtils.loadDB(tmp, pass, keyFileInfo);
        DBUtils.exportDb(db2, new File("/tmp/testUpdateSaveExport2.json"));
    }

    //@Test
    public void testAllHeaderKeys() throws Exception {
        String encrypted = "JIHouApJhZm9VxN2pppRag==";
        byte[] encryptedBytes = Base64.decodeBase64(encrypted.getBytes());
        File tmp = new File("/home/starksm/tmp");
        //File[] dbFiles = tmp.listFiles((File f) -> f.getName().startsWith("HWupdate.2019"));
        File[] dbFiles = {new File("/home/starksm/tmp/SIKeyPass.kdbx"),
                new File("/home/starksm/tmp/SavedKeyPass.kdbx"),
                new File("/home/starksm/tmp/HWupdate"),
                new File(USB_PATH+"/SIKeyPass.kdbx"),
                new File("/home/starksm/Applications/kp/SIKeyPass.kdbx")
        };
        tmp = new File("/home/starksm/Applications/kp");
        dbFiles = tmp.listFiles((File f) -> f.getName().startsWith("SIKeyPass."));
        for(File db : dbFiles) {
            FileInputStream is = new FileInputStream(db);
            KdbxHeader kdbxHeader = KdbxSerializer.readOuterHeader(is, new KdbxHeader(3, true));
            is.close();
            StreamEncryptor encryptor = kdbxHeader.getStreamEncryptor();
            byte[] decrypt = encryptor.decrypt(encryptedBytes);
            System.out.printf("%s to: %s\n", db.getName(), new String(decrypt, StandardCharsets.UTF_8));
        }
    }
    @Test
    public void testAllHeaders() throws Exception {
        String encrypted = "2PRAo6nJkgEv2UXFQA==";
        byte[] encryptedBytes = Base64.decodeBase64(encrypted.getBytes());
        File tmp = new File("/home/starksm/tmp");
        File[] dbFiles = tmp.listFiles((File f) -> f.getName().startsWith("HWupdate.2019"));
        for(File db : dbFiles) {
            FileInputStream is = new FileInputStream(db);
            KdbxHeader kdbxHeader = KdbxSerializer.readOuterHeader(is, new KdbxHeader(3, true));
            is.close();
            System.out.printf("cipher=%s, irsk=%s\n", kdbxHeader.getCipherUuid(), new BigInteger(kdbxHeader.getInnerRandomStreamKey()));
        }
    }
    @Test
    public void testExportAll() throws Exception {
        String[] pathInfo = DBUtils.loadDefault();
        String pass = pathInfo[1];
        String keyFileInfo = pathInfo[2];
        File tmp = new File("/home/starksm/tmp");
        File[] dbFiles = tmp.listFiles((File f) -> f.getName().startsWith("HWupdate.2019"));
        for(File db : dbFiles) {
            JaxbDatabase testDb = DBUtils.loadDB(db, pass, keyFileInfo);
            DBUtils.exportDb(testDb, new File("/tmp/" + db.getName() + ".xml"));
            System.out.printf("Exported %s\n", db.getName());
        }
    }
    @Test
    public void testExportHWupdateGood() throws Exception {
        String[] pathInfo = DBUtils.loadDefault();
        String pass = pathInfo[1];
        String keyFileInfo = pathInfo[2];
        File db = new File("/home/starksm/tmp/HWupdate_good.kdbx");
        JaxbDatabase testDb = DBUtils.loadDB(db, pass, keyFileInfo);
        DBUtils.exportDb(testDb, new File("/tmp/" + db.getName() + ".xml"));
        System.out.printf("Exported %s\n", db.getName());
    }
    @Test
    public void testEncryption() throws Exception {
        // Needs a default.map on test system
        String[] pathInfo = DBUtils.loadDefault();
        String pass = pathInfo[1];
        String keyFileInfo = pathInfo[2];
        Credentials credentials = DBUtils.createCredentials(pass, keyFileInfo);
        KdbxHeader kdbxHeader = new KdbxHeader();
        FileInputStream encryptedInputStream = new FileInputStream(USB_PATH + "/SIKeyPass.kdbx");
        InputStream decryptedInputStream = KdbxSerializer.createUnencryptedInputStream(credentials, kdbxHeader, encryptedInputStream);
        StreamEncryptor encryptor = kdbxHeader.getStreamEncryptor();
        String encrypted = "2PRAo6nJkgEv2UXFQA==";
        byte[] encryptedBytes = Base64.decodeBase64(encrypted.getBytes());
        byte[] decrypt = encryptor.decrypt(encryptedBytes);
        System.out.printf("#1 %s to: %s\n", encrypted, new String(decrypt, StandardCharsets.UTF_8));
    }

    //@Test
    public void testXmlDiffs() throws Exception {
        Diff myDiff = DiffBuilder.compare(Input.fromFile(new File("/home/starksm/tmp/HWupdate.json")))
                .withTest(Input.fromFile(new File("/home/starksm/tmp/SavedKeyPass.json")))
                .checkForSimilar()
                .ignoreWhitespace()
                .build();
        FileWriter diffs = new FileWriter("/tmp/testXmlDiffs.txt");
        for(Difference diff : myDiff.getDifferences()) {
            diffs.write(diff.toString());
        }
    }
}
