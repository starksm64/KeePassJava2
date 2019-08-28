package com.si.keypass;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import org.linguafranca.pwdb.Credentials;
import org.linguafranca.pwdb.kdbx.*;
import org.linguafranca.pwdb.kdbx.jaxb.JaxbSerializableDatabase;
import org.linguafranca.pwdb.kdbx.jaxb.binding.Binaries;
import org.linguafranca.pwdb.kdbx.jaxb.binding.BinaryField;
import org.linguafranca.pwdb.kdbx.jaxb.binding.JaxbEntryBinding;
import org.linguafranca.pwdb.kdbx.jaxb.binding.JaxbGroupBinding;
import org.linguafranca.pwdb.kdbx.jaxb.binding.KeePassFile;

public class KeePassController {
    @FXML
    TreeView<JaxbGroupBinding> groupTreeView;
    @FXML
    TableView<KeePassEntry> entryTableView;

    @FXML
    TableColumn<KeePassEntry, ImageView> imageColumn;
    @FXML
    TableColumn<KeePassEntry, String> titleColumn;
    @FXML
    TableColumn<KeePassEntry, String> usernameColumn;
    @FXML
    TableColumn<KeePassEntry, String> urlColumn;
    @FXML
    TextArea notesArea;
    @FXML
    ListView<KeePassAttachment> attributesList;
    @FXML
    TextField passwordField;
    @FXML
    TextField repeatField;
    @FXML
    MenuItem extractMenuItem;
    private SimpleBooleanProperty hasAttachments = new SimpleBooleanProperty();

    private TreeItem<JaxbGroupBinding> rootGroup;
    private Map<UUID, javafx.scene.image.ImageView> iconsMap;
    private KeePassFile keePassFile;
    private String username;
    private String password;
    private AtomicBoolean isModified = new AtomicBoolean();

    public void setRoot(TreeItem<JaxbGroupBinding> rootGroup, Map<UUID, ImageView> iconsMap, KeePassFile keePassFile) {
        this.rootGroup = rootGroup;
        this.iconsMap = iconsMap;
        this.keePassFile = keePassFile;
        groupTreeView.setRoot(rootGroup);
        groupTreeView.setCellFactory(new Callback<TreeView<JaxbGroupBinding>, TreeCell<JaxbGroupBinding>>(){
            @Override
            public TreeCell<JaxbGroupBinding> call(TreeView<JaxbGroupBinding> p) {
                return new TreeCell<JaxbGroupBinding>() {
                    @Override
                    protected void updateItem(JaxbGroupBinding item, boolean empty) {
                        super.updateItem(item, empty);
                        if(item != null) {
                            setText(item.getName());
                            UUID uuid = item.getCustomIconUUID();
                            if(uuid != null) {
                                javafx.scene.image.ImageView iconView = iconsMap.get(uuid);
                                if (iconView != null) {
                                    setGraphic(iconView);
                                    System.out.printf("Set %s icon to: %s\n", item.getName(), item.getCustomIconUUID());
                                }
                            }
                        }
                    }
                };
            }
        });
        groupTreeView.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> groupSelected(newValue.getValue()));

        entryTableView.setItems(FXCollections.observableArrayList());
        imageColumn.setCellValueFactory(cdf -> cdf.getValue().getIcon());
        titleColumn.setCellValueFactory(cdf -> cdf.getValue().getTitle());
        usernameColumn.setCellValueFactory(cdf -> cdf.getValue().getName());
        urlColumn.setCellValueFactory(cdf -> cdf.getValue().getURLValue());
        entryTableView.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> entrySelected(newValue));
        extractMenuItem.disableProperty().bind(hasAttachments.not());
    }


    @FXML
    private void initialize() {
        System.out.printf("KeePassController.initialize\n");
    }

    private void groupSelected(JaxbGroupBinding group) {
        System.out.printf("groupSelected: %s\n", group.getName());
        List<JaxbEntryBinding> bindings = group.getEntry();
        ArrayList<KeePassEntry> tmp = new ArrayList<>();
        for(JaxbEntryBinding binding : bindings) {
            KeePassEntry entry = new KeePassEntry(group, binding, iconsMap);
            for(BinaryField binaryField : binding.getBinary()) {
                String name = binaryField.getKey();
                Integer ref = binaryField.getValue().getRef();
                byte[] data = null;
                for (Binaries.Binary binary: keePassFile.getMeta().getBinaries().getBinary()){
                    if (binary.getID().equals(ref)) {
                        if (binary.getCompressed()) {
                            data = Helpers.unzipBinaryContent(binary.getValue());
                        } else {
                            data = binary.getValue();
                        }
                    }
                }
                KeePassAttachment attachment = new KeePassAttachment(name, data);
                entry.getAttachments().add(attachment);
            }
            System.out.println(entry);
            tmp.add(entry);
        }
        ObservableList<KeePassEntry> entryBindings = FXCollections.observableArrayList(tmp);
        entryTableView.setItems(entryBindings);
    }

    private void entrySelected(KeePassEntry entry) {
        hasAttachments.set(false);
        if(entry != null) {
            System.out.printf("entrySelected: %s\n", entry.getTitle());
            String notes = entry.getNotes();
            notesArea.setText(notes);
            passwordField.setText(entry.getPassword());
            List<KeePassAttachment> attachments = entry.getAttachments();
            if(attachments.size() > 0) {
                attributesList.getItems().clear();
                attributesList.getItems().addAll(attachments);
                hasAttachments.set(true);
                System.out.printf("Extract should be enabled\n");
            }
        }
    }

    @FXML
    private void copyPassword() {
        KeePassEntry selected = entryTableView.getSelectionModel().getSelectedItem();
        if(selected != null) {
            String value = selected.getPassword();
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(value);
            clipboard.setContent(content);
            System.out.printf("copyUsername, %s\n", value);
        }
    }
    @FXML
    private void copyUsername() {
        KeePassEntry selected = entryTableView.getSelectionModel().getSelectedItem();
        if(selected != null) {
            String value = selected.getUsername();
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(value);
            clipboard.setContent(content);
            System.out.printf("copyUsername, %s\n", value);
        }
    }
    @FXML
    private void copyURL() {
        KeePassEntry selected = entryTableView.getSelectionModel().getSelectedItem();
        if(selected != null) {
            String value = selected.getURL();
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(value);
            content.put(DataFormat.URL, value);
            clipboard.setContent(content);
            System.out.printf("copyUsername, %s\n", value);
        }
    }

    @FXML
    private void extractAttachment() {
        KeePassAttachment attachment = attributesList.getSelectionModel().getSelectedItem();
        if(attachment != null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialFileName(attachment.getName());
            File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                try {
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(attachment.getData());
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @FXML
    private void fileSave() {

    }
    @FXML
    private void fileQuit() {
        // TODO: check for modifications
        if(isModified.get()) {

        }
        Platform.exit();
    }
    private void saveChanges() throws IOException {
        FileChooser fc = new FileChooser();
        File saveFile = fc.showSaveDialog(null);
        if(saveFile != null) {
            OutputStream testStream = new FileOutputStream(saveFile);
            // Read the password from /tmp/testLoadDB.pass
            FileReader reader = new FileReader("/tmp/testLoadDB.pass");
            BufferedReader br = new BufferedReader(reader);
            String pass = br.readLine();
            br.close();
            CompositeKey credentials = new CompositeKey();
            KdbxCreds creds = new KdbxCreds(pass.getBytes());
            OutputStream outputStream = KdbxSerializer.createEncryptedOutputStream(credentials, new KdbxHeader(), testStream);
            JaxbSerializableDatabase db = new JaxbSerializableDatabase();
        }
    }
}
