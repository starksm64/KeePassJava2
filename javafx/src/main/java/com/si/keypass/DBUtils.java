package com.si.keypass;

import org.linguafranca.pwdb.Credentials;
import org.linguafranca.pwdb.kdbx.CompositeKey;
import org.linguafranca.pwdb.kdbx.HashedKey;
import org.linguafranca.pwdb.kdbx.Helpers;
import org.linguafranca.pwdb.kdbx.KdbxCreds;
import org.linguafranca.pwdb.kdbx.KdbxHeader;
import org.linguafranca.pwdb.kdbx.jaxb.JaxbDatabase;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DBUtils {
    /**
     * Load a primary password from a file
     * @param selectedFile - password file
     * @return password
     * @throws IOException - on read failure
     */
    public static String loadPasswordFromFile(File selectedFile) throws IOException {
        FileReader reader = new FileReader(selectedFile);
        BufferedReader br = new BufferedReader(reader);
        String pass = br.readLine();
        pass = pass + pass;
        br.close();
        return pass;
    }

    /**
     * Load a primary password from a yubikey using yubikey-piv-tool
     * @param pivToolPath path to yubikey-piv-tool
     * @return passsword, null on failure
     */
    public static String loadPasswordFromYubikey(String pivToolPath) {
        ProcessBuilder pb = new ProcessBuilder();
        pb.environment().put("LD_LIBRARY_PATH", "/usr/local/lib");
        ArrayList<String> command = new ArrayList<>();

        command.add(pivToolPath);
        command.add("-a");
        command.add("read-object");
        command.add("--id");
        command.add("0x5fc10d");
        System.out.printf("Running %s\n", command);
        pb.command(command);
        String data = null;
        int ok = 0;
        try {
            System.out.printf("Starting %s\n", pb);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            data = reader.readLine();
            ok = process.waitFor();
            System.out.printf("yubico-piv-tool exit status: %d\n", ok);
            if(ok == 0) {
                System.out.printf("Read %d bytes of data\n", data.length());
            }
        } catch (IOException |InterruptedException e) {
            System.out.printf("Yubikey Error, %s", e);
        }

        return data;
    }

    /**
     * Read a default.map file to determine the default pass file, etc.
     * @param defaultMap - default.map file to load
     * @return the file names for the defaults if found, null otherwise
     */
    public static String[] readDefaultMap(File defaultMap) throws IOException {
        try(BufferedReader reader = new BufferedReader(new FileReader(defaultMap))) {
            List<String> tmp = reader.lines().collect(Collectors.toList());
            String[] lines = new String[tmp.size()];
            tmp.toArray(lines);
            return lines;
        }
    }

    /**
     * Get default db information from a default map file
     * @return [0] - path to db, [1] = password, [2] = key files
     */
    public static String[] loadDefault() throws IOException {
        String dbFile = null;
        String password = null;
        String keyFiles = null;
        // Try the three different roots...
        String[] roots = {"/home/starksm/Applications/kp/", "/media/starksm/Samsung USB/", "/Users/starksm/private/"};
        for(String rootDir : roots) {
            // Look for a default.map file
            File defaultMap = new File(rootDir, "default.map");
            if (defaultMap.canRead()) {
                System.out.printf("Loading defaults from: %s\n", defaultMap.getAbsolutePath());
                String[] files = null;
                try {
                    files = DBUtils.readDefaultMap(defaultMap);
                } catch (IOException e) {
                    Dialogs.showExceptionAlert("DefaultMap Failure", "Failed to read: "+defaultMap.getAbsolutePath(), e);
                }
                System.out.printf("Read default files: %s\n", Arrays.asList(files));
                if(files == null) {
                    System.err.print("Failed to load defaults, no default.map found\n");
                    break;
                }
                password = loadPasswordFromFile(new File(rootDir, files[0]));
                StringBuilder tmp = new StringBuilder();
                for (int n = 1; n < files.length; n ++) {
                    String f = files[n];
                    tmp.append(rootDir);
                    tmp.append(f);
                    tmp.append(',');
                }
                tmp.setLength(tmp.length() - 1);
                keyFiles = tmp.toString();
                dbFile = rootDir + "SIKeyPass.kdbx";
                break;
            }
        }
        return new String[]{dbFile, password, keyFiles};
    }

    /**
     * Build up composite credentials information
     * @param pass - primary password
     * @param keyFilesText - concatenated key file names
     * @return composite credentials instance
     * @throws IOException - on failure
     */
    public static Credentials createCredentials(String pass, String keyFilesText) throws IOException {
        CompositeKey dbCredentials = new CompositeKey();
        KdbxCreds creds = new KdbxCreds(pass.getBytes());
        System.out.printf("KdbxCreds: %s\n", Helpers.encodeBase64Content(creds.getKey()));
        dbCredentials.addKey(creds);
        String[] keyFiles = keyFilesText.split(",");
        System.out.printf("keyFileField: %s\n", keyFilesText);
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
            dbCredentials.addKey(hkey);
        }
        return dbCredentials;
    }

    /**
     *
     * @param kdbxFile
     * @param pass
     * @param keyFileInfo
     * @return
     * @throws IOException
     */
    public static JaxbDatabase loadDB(File kdbxFile, String pass, String keyFileInfo) throws IOException {
        // Now open the DB
        FileInputStream kdbxIS = new FileInputStream(kdbxFile);
        Credentials credentials = createCredentials(pass, keyFileInfo);
        System.out.printf(credentials.toString());
        JaxbDatabase keePassDB = JaxbDatabase.load(credentials, kdbxIS);
        return keePassDB;
    }

    public static void exportDb(JaxbDatabase db, File saveFile) throws IOException {
        NoopJaxbSDb tmpDB = new NoopJaxbSDb();
        tmpDB.setKeePassFile(db.getKeePassFile());
        OutputStream exportStream = new FileOutputStream(saveFile);
        tmpDB.save(exportStream);
        exportStream.flush();
        exportStream.close();
        //keyPassDB.save(new StreamFormat.None(), null, testStream);
        System.out.printf("Exported(%d) to: %s\n", saveFile.length(), saveFile.getAbsolutePath());
    }
    public static void saveDb(JaxbDatabase db, File saveFile, Credentials credentials) throws IOException {
        OutputStream saveStream = new FileOutputStream(saveFile);
        db.save(credentials, saveStream);
        saveStream.flush();
        saveStream.close();
        System.out.printf("Saved(%d) to: %s\n", saveFile.length(), saveFile.getAbsolutePath());
    }
}
