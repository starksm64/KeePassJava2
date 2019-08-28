package com.si.keypass;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.linguafranca.pwdb.kdbx.CompositeKey;
import org.linguafranca.pwdb.kdbx.HashedKey;
import org.linguafranca.pwdb.kdbx.Helpers;
import org.linguafranca.pwdb.kdbx.KdbxCreds;
import org.linguafranca.pwdb.kdbx.KdbxHeader;
import org.linguafranca.pwdb.kdbx.KdbxSerializer;
import org.linguafranca.pwdb.kdbx.jaxb.JaxbSerializableDatabase;
import org.linguafranca.pwdb.kdbx.jaxb.binding.CustomIcons;
import org.linguafranca.pwdb.kdbx.jaxb.binding.JaxbEntryBinding;
import org.linguafranca.pwdb.kdbx.jaxb.binding.JaxbGroupBinding;
import org.linguafranca.pwdb.kdbx.jaxb.binding.KeePassFile;
import org.linguafranca.pwdb.kdbx.jaxb.binding.StringField;

public class KeePassFX extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        TreeView treeView = loadDB();

        VBox vbox = new VBox(treeView);

        Scene scene = new Scene(vbox);

        primaryStage.setScene(scene);

        primaryStage.show();
    }

    private TreeView loadDB() throws IOException {
        // get an input stream from KDB file
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

        // Icon map
        List<CustomIcons.Icon> icons = dbFile.getMeta().getCustomIcons().getIcon();
        Map<UUID, ImageView> iconsMap = new HashMap<>();
        for (CustomIcons.Icon icon : icons) {
            ByteArrayInputStream data = new ByteArrayInputStream(icon.getData());
            ImageView iconView = new ImageView(new Image(data));
            iconsMap.put(icon.getUUID(), iconView);
        }

        TreeItem rootItem = new TreeItem("Root");
        for(JaxbGroupBinding group : groups) {
            TreeItem groupItem = toTreeItem(group, iconsMap);
            ImageView iconView = iconsMap.get(group.getCustomIconUUID());
            if(iconView != null) {
                groupItem.setGraphic(iconView);
            }
            rootItem.getChildren().add(groupItem);
        }
        System.out.println(output.toString());

        TreeView treeView = new TreeView();
        treeView.setRoot(rootItem);
        return treeView;
    }

    private TreeItem toTreeItem(JaxbGroupBinding group, Map<UUID, ImageView> iconsMap) {
        TreeItem groupItem = new TreeItem(group.getName());
        if(group.getCustomIconUUID() != null) {
            String iconUuid = Helpers.base64FromUuid(group.getCustomIconUUID());
        }

        for(JaxbEntryBinding entry : group.getEntry()) {
            TreeItem entryItem = toTreeItem(entry, iconsMap);
            if(entryItem != null) {
                groupItem.getChildren().add(entryItem);
            }
        }
        return groupItem;
    }
    private TreeItem toTreeItem(JaxbEntryBinding entry, Map<UUID, ImageView> iconsMap) {
        TreeItem entryItem = null;

        for(StringField field : entry.getString()) {
            if(field.getKey().equals("Title")) {
                entryItem = new TreeItem(field.getValue().getValue());
                ImageView iconView = iconsMap.get(entry.getCustomIconUUID());
                if(iconView != null) {
                    entryItem.setGraphic(iconView);
                }

            }
        }
        return entryItem;
    }

}
