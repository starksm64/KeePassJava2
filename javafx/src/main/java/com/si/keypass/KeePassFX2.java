package com.si.keypass;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.linguafranca.pwdb.Credentials;
import org.linguafranca.pwdb.kdbx.CompositeKey;
import org.linguafranca.pwdb.kdbx.HashedKey;
import org.linguafranca.pwdb.kdbx.Helpers;
import org.linguafranca.pwdb.kdbx.KdbxCreds;
import org.linguafranca.pwdb.kdbx.KdbxHeader;
import org.linguafranca.pwdb.kdbx.KdbxSerializer;
import org.linguafranca.pwdb.kdbx.jaxb.JaxbSerializableDatabase;
import org.linguafranca.pwdb.kdbx.jaxb.binding.CustomIcons;
import org.linguafranca.pwdb.kdbx.jaxb.binding.JaxbGroupBinding;
import org.linguafranca.pwdb.kdbx.jaxb.binding.KeePassFile;

public class KeePassFX2 extends Application {
    Map<UUID, ImageView> iconsMap = new HashMap<>();
    KeePassFile keePassFile;

    @Override
    public void start(Stage primaryStage) throws Exception {

        URL fxml = getClass().getResource("/main.fxml");
        FXMLLoader loader = new FXMLLoader(fxml);
        Parent root = loader.load();
        KeePassController controller = loader.getController();
        TreeItem<JaxbGroupBinding> rootGroup = loadDB();
        controller.setRoot(rootGroup, iconsMap, keePassFile);
        System.out.printf("root=%s, controller=%s\n", root, controller);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private TreeItem<JaxbGroupBinding> loadDB() throws IOException {
        // get an input stream from KDB file
        String rootDir = "/media/starksm/Samsung USB/";
        //String rootDir = "/home/starksm/Applications/kp/";
        // A test dbx with a password of KeyPass.kdbx
        String kdbxFile = "SIKeyPass.kdbx";
        FileInputStream kdbxIS = new FileInputStream(rootDir+kdbxFile);
        Credentials credentials = createCredentials(rootDir);
        System.out.printf(credentials.toString());
        KdbxHeader kdbxHeader = new KdbxHeader();
        System.out.printf("Header.getCipherUuid: %s\n", kdbxHeader.getCipherUuid());
        System.out.printf("Header.getProtectedStreamAlgorithm: %s\n", kdbxHeader.getProtectedStreamAlgorithm());
        System.out.printf("Header.getVersion: %s\n", kdbxHeader.getVersion());
        InputStream decryptedInputStream = KdbxSerializer.createUnencryptedInputStream(credentials, kdbxHeader, kdbxIS);
        JaxbSerializableDatabase db = new JaxbSerializableDatabase();
        db.setEncryption(kdbxHeader.getStreamEncryptor());
        db.load(decryptedInputStream);
        keePassFile = db.getKeePassFile();
        KeePassFile.Root root = keePassFile.getRoot();
        List<JaxbGroupBinding> groups = root.getGroup().getGroup();
        StringBuilder output = new StringBuilder();
        output.append("SIKeyPass.kdbx\n" );

        // Icon map
        List<CustomIcons.Icon> icons = keePassFile.getMeta().getCustomIcons().getIcon();
        for (CustomIcons.Icon icon : icons) {
            ByteArrayInputStream data = new ByteArrayInputStream(icon.getData());
            ImageView iconView = new ImageView(new Image(data, 64, 64, false, false));
            iconsMap.put(icon.getUUID(), iconView);
        }

        TreeItem<JaxbGroupBinding> rootItem = new TreeItem<>(root.getGroup());
        for(JaxbGroupBinding group : groups) {
            TreeItem<JaxbGroupBinding> groupItem = toTreeItem(group, iconsMap);
            ImageView iconView = iconsMap.get(group.getCustomIconUUID());
            if(iconView != null) {
                groupItem.setGraphic(iconView);
            }
            rootItem.getChildren().add(groupItem);
        }
        System.out.println(output.toString());

        return rootItem;
    }

    private TreeItem<JaxbGroupBinding> toTreeItem(JaxbGroupBinding group, Map<UUID, ImageView> iconsMap) {
        TreeItem<JaxbGroupBinding> groupItem = new TreeItem<>(group);
        if(group.getCustomIconUUID() != null) {
            ImageView iconView = iconsMap.get(group.getCustomIconUUID());
            if(iconView != null) {
                groupItem.setGraphic(iconView);
                System.out.printf("Set %s icon to: %s\n", group.getName(), group.getCustomIconUUID());
            }
        }
        return groupItem;
    }

    static Credentials createCredentials(String rootDir) throws IOException {
        // Read the password from /tmp/testLoadDB.pass
        //FileReader reader = new FileReader("/tmp/testLoadDB.pass");
        FileReader reader = new FileReader(rootDir+"x");
        BufferedReader br = new BufferedReader(reader);
        String pass = br.readLine();
        br.close();
        CompositeKey credentials = new CompositeKey();
        pass = pass + pass;
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
        return credentials;
    }
}

