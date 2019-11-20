package com.si.keypass;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.si.keypass.prefs.PrefsUtils;
import io.jsondb.JsonDBTemplate;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.linguafranca.pwdb.Credentials;
import org.linguafranca.pwdb.kdbx.CompositeKey;
import org.linguafranca.pwdb.kdbx.HashedKey;
import org.linguafranca.pwdb.kdbx.Helpers;
import org.linguafranca.pwdb.kdbx.KdbxCreds;
import org.linguafranca.pwdb.kdbx.jaxb.JaxbDatabase;
import org.linguafranca.pwdb.kdbx.jaxb.JaxbGroup;
import org.linguafranca.pwdb.kdbx.jaxb.binding.CustomIcons;
import org.linguafranca.pwdb.kdbx.jaxb.binding.KeePassFile;

/**
 * The current JavaFX application entry point for the KeyPass app.
 */
public class KeePassFX2 extends Application {
    static DateTimeFormatter NOW_TIME_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS");
    private Map<UUID, ImageView> iconsMap = new HashMap<>();
    private List<CustomIcons.Icon> icons;
    // This will be override by prefs if they exist
    private String pivToolPath = "/Users/starksm/bin/yubico-piv-tool-1.7.0/bin/yubico-piv-tool";
    private JaxbDatabase keePassDB;
    private KeePassFile keePassFile;
    private CompositeKey dbCredentials;
    @FXML
    Label loadDlogTitle;
    @FXML
    RadioButton passwordBtn;
    @FXML
    PasswordField passwordField;
    @FXML
    TextField keyFileField;
    @FXML
    TextField dbFileField;

    // Was the load db dialog cancelled
    private boolean loadCancelled;
    private Stage loadDbStage;
    private KeePassController controller;
    private AppPrefs appPrefs;

    public static void main(String[] args) {
        launch(args);
    }
    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.printf("KeyPassFX2.start...\n");

        URL fxml = getClass().getResource("/main.fxml");
        FXMLLoader loader = new FXMLLoader(fxml);
        Parent root = loader.load();
        controller = loader.getController();
        TreeItem<JaxbGroup> rootGroup = loadDB();
        if(rootGroup != null) {
            controller.setRoot(keePassDB, rootGroup, iconsMap, icons, dbCredentials);
            System.out.printf("root=%s, controller=%s\n", root, controller);
            Scene scene = new Scene(root);
            scene.focusOwnerProperty().addListener((obs, old, nv) -> {System.out.printf("focus(%s), %s to %s\n", obs, old, nv); controller.setFocusNode(nv);});
            // Set the application icon for the docks
            URL appPng = getClass().getResource("/icons/keepassxc.png");
            if(appPng != null) {
                System.out.printf("Found icon: %s\n", appPng);
                Image mainIcon = new Image(appPng.openStream());
                primaryStage.getIcons().add(mainIcon);
            } else {
                System.out.printf("No app icon: %s\n", getClass().getResource("icons/keypassxc.png"));
            }
            primaryStage.setTitle(dbFileField.getText());
            primaryStage.setScene(scene);
            primaryStage.show();
        }
    }

    @FXML
    private void initialize() {
        System.out.printf("KeePassFX2.initialize\n");
        try {
            JsonDBTemplate dbTemplate = PrefsUtils.getPreferences(AppPrefs.class);
            appPrefs = dbTemplate.findById("KeePassJava2Prefs", AppPrefs.class);
        } catch (Exception e) {
            Dialogs.showExceptionAlert("AppPrefs", "Failed to load AppPrefs", e);
            appPrefs = new AppPrefs();
        }
    }

    @FXML
    private void togglePassword() {
        String label;
        if(passwordBtn.isSelected()) {
            label = "Password";
        } else {
            label = "YubiKey";
        }
    }
    @FXML
    private void selectPassword() {
        if(passwordBtn.isSelected()) {
            loadPasswordFromFile();
        } else {
            loadPasswordFromYubikey();
        }
    }

    private void loadPasswordFromFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Password File(s)");
        File selectedFile = fileChooser.showOpenDialog(null);
        if(selectedFile != null) {
            loadPasswordFromFile(selectedFile);
        }
    }
    private void loadPasswordFromFile(File selectedFile) {
        try {
            String pass = DBUtils.loadPasswordFromFile(selectedFile);
            passwordField.setText(pass);
        } catch (IOException e) {
            Dialogs.showExceptionAlert("Password from File Error", selectedFile.getAbsolutePath(), e);
        }
    }

    /**
     * Load the pw from a yubikey
     */
    private void loadPasswordFromYubikey() {
        ProcessBuilder pb = new ProcessBuilder();
        pb.environment().put("LD_LIBRARY_PATH", "/usr/local/lib");
        ArrayList<String> command = new ArrayList<>();

        command.add(appPrefs.getYubitoolPath());
        command.add("-a");
        command.add("read-object");
        command.add("--id");
        command.add("0x5fc10d");
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
        } catch (IOException|InterruptedException e) {
            Dialogs.showExceptionAlert("Yubikey Error", pivToolPath, e);
        }

        if(ok == 0) {
            // Convert from hex to decimal string
            StringBuilder tmp = new StringBuilder();
            for (int n = 0; n < data.length(); n += 2) {
                String digits = data.substring(n, n + 2);
                int value = Integer.parseInt(digits, 16);
                tmp.append((char) value);
            }
            tmp.append(tmp.toString());
            Platform.runLater(() -> passwordField.setText(tmp.toString()));
        } else {
            Dialogs.showWarning("Failed to Read YubiKey", String.format("yubico-piv-tool exit status: %d\n", ok));
        }
    }

    @FXML
    private void selectKeys() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Database Key File(s)");
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null);
        if(selectedFiles != null) {
            StringBuilder list = new StringBuilder();
            for(File f : selectedFiles) {
                list.append(f.getAbsolutePath());
                list.append(',');
            }
            list.setLength(list.length()-1);
            keyFileField.setText(list.toString());
        }
    }
    @FXML
    private void selectDBFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select KeyPass kdbx file");
        fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("KeyPass", ".kdbx"));
        File selectedFile = fileChooser.showOpenDialog(null);
        if(selectedFile != null) {
            dbFileField.setText(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    private void cancelDBLoadDialog() {
        loadCancelled = true;
        loadDlogTitle.setText("Cancelling...");
        loadDbStage.hide();
    }
    @FXML
    private void completeDBLoadDialog() {
        loadCancelled = false;
        loadDlogTitle.setText("Loading...");
        loadDbStage.hide();
    }

    private TreeItem<JaxbGroup> loadDB() throws IOException {
        URL fxml = getClass().getResource("/loaddb.fxml");
        FXMLLoader loader = new FXMLLoader(fxml);
        //loader.setRoot(this);
        loader.setController(this);
        Parent parent = loader.load();
        Scene scene = new Scene(parent, 600, 300);
        loadDbStage = new Stage();
        loadDbStage.initModality(Modality.APPLICATION_MODAL);
        loadDbStage.setScene(scene);
        loadDbStage.showAndWait();
        // If the dialog was cancelled, exit
        if(loadCancelled) {
            Dialogs.showWarning("Load was cancelled", "Load cancelled, exiting...");
            Platform.exit();
            return null;
        }
        scene.setCursor(Cursor.WAIT);
        loadDbStage.toBack();
        loadDbStage = null;

        TreeItem<JaxbGroup> rootItem = null;

        // get an input stream from KDB file
        String kdbxFileName =  dbFileField.getText();
        if(kdbxFileName == null || kdbxFileName.length() == 0) {
            kdbxFileName = loadDefault();
            dbFileField.setText(kdbxFileName);
        }
        if(kdbxFileName != null) {
            // First make a backup of the file
            File kdbxFile = new File(kdbxFileName);
            LocalDateTime now = LocalDateTime.now();
            String backupName = kdbxFileName + "." + now.format(NOW_TIME_FMT);
            File keePassFileBackup = new File(backupName);
            try {
                Files.copy(kdbxFile.toPath(), keePassFileBackup.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
                System.out.printf("Made a copy of DB as: %s\n", keePassFileBackup.getAbsolutePath());
                controller.setBackupFile(keePassFileBackup);
            } catch (IOException e) {
                Dialogs.showExceptionAlert("Backup failure", backupName, e);
            }

            // Now open the DB
            FileInputStream kdbxIS = new FileInputStream(kdbxFile);
            Credentials credentials = createCredentials();
            System.out.printf(credentials.toString());
            keePassDB = JaxbDatabase.load(credentials, kdbxIS);
            /*
            InputStream decryptedInputStream = KdbxSerializer.createUnencryptedInputStream(credentials, kdbxHeader, kdbxIS);
            JaxbSerializableDatabase db = new JaxbSerializableDatabase();
            db.setEncryption(kdbxHeader.getStreamEncryptor());
            db.load(decryptedInputStream);
            */
            keePassFile = keePassDB.getKeePassFile();
            List<JaxbGroup> groups = keePassDB.getRootGroup().getGroups();
            StringBuilder output = new StringBuilder();
            output.append("*.kdbx\n" );

            // Icon map
            icons = keePassFile.getMeta().getCustomIcons().getIcon();
            RandomAccessFile iconCache = new RandomAccessFile("/tmp/icon.cache", "rw");
            for (CustomIcons.Icon icon : icons) {
                iconCache.writeUTF(icon.getUUID().toString());
                iconCache.writeInt(icon.getData().length);
                iconCache.write(icon.getData());
                ByteArrayInputStream data = new ByteArrayInputStream(icon.getData());
                ImageView iconView = new ImageView(new Image(data, 64, 64, false, false));
                iconsMap.put(icon.getUUID(), iconView);
            }
            iconCache.close();

            rootItem = new TreeItem<>(keePassDB.getRootGroup());
            for(JaxbGroup group : groups) {
                TreeItem<JaxbGroup> groupItem = toTreeItem(group, iconsMap);
                ImageView iconView = iconsMap.get(group.getCustomIconUUID());
                if(iconView != null) {
                    groupItem.setGraphic(iconView);
                }
                rootItem.getChildren().add(groupItem);
            }
            System.out.println(output.toString());
            controller.addRecentFile(kdbxFileName);
        }
        scene.setCursor(Cursor.DEFAULT);

        return rootItem;
    }
    private String loadDefault() {
        String dbFile = null;
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
                    Dialogs.showWarning("Failed to load defaults", "No default.map found");
                    return null;
                }
                loadPasswordFromFile(new File(rootDir, files[0]));
                StringBuilder tmp = new StringBuilder();
                for (int n = 1; n < files.length; n ++) {
                    String f = files[n];
                    tmp.append(rootDir + f);
                    tmp.append(',');
                }
                tmp.setLength(tmp.length() - 1);
                keyFileField.setText(tmp.toString());
                dbFile = rootDir + "SIKeyPass.kdbx";
                break;
            }
        }
        return dbFile;
    }



    private TreeItem<JaxbGroup> toTreeItem(JaxbGroup group, Map<UUID, ImageView> iconsMap) {
        TreeItem<JaxbGroup> groupItem = new TreeItem<>(group);
        if(group.getCustomIconUUID() != null) {
            ImageView iconView = iconsMap.get(group.getCustomIconUUID());
            if(iconView != null) {
                groupItem.setGraphic(iconView);
                System.out.printf("Set %s icon to: %s\n", group.getName(), group.getCustomIconUUID());
            }
        }
        return groupItem;
    }

    Credentials createCredentials() throws IOException {
        dbCredentials = new CompositeKey();
        String pass = passwordField.getText();
        KdbxCreds creds = new KdbxCreds(pass.getBytes());
        System.out.printf("KdbxCreds: %s\n", Helpers.encodeBase64Content(creds.getKey()));
        dbCredentials.addKey(creds);
        String[] keyFiles = keyFileField.getText().split(",");
        System.out.printf("keyFileField: %s\n", keyFileField.getText());
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
}

