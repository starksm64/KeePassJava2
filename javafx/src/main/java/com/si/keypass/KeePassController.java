package com.si.keypass;

import java.io.*;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.si.keypass.prefs.PrefsUtils;
import io.jsondb.JsonDBTemplate;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import javafx.util.Pair;
import org.linguafranca.pwdb.kdbx.*;
import org.linguafranca.pwdb.kdbx.jaxb.JaxbSerializableDatabase;
import org.linguafranca.pwdb.kdbx.jaxb.binding.Binaries;
import org.linguafranca.pwdb.kdbx.jaxb.binding.BinaryField;
import org.linguafranca.pwdb.kdbx.jaxb.binding.JaxbEntryBinding;
import org.linguafranca.pwdb.kdbx.jaxb.binding.JaxbGroupBinding;
import org.linguafranca.pwdb.kdbx.jaxb.binding.KeePassFile;
import org.linguafranca.pwdb.kdbx.jaxb.binding.StringField;

public class KeePassController {
    @FXML
    MenuBar menuBar;
    @FXML
    Menu recentFilesMenu;
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
    ListView<KeePassAttachment> attachmentList;
    @FXML
    TableView<Pair<String, String>> attributesTable;
    @FXML
    TableColumn<Pair<String, String>, String> attrNameColumn;
    @FXML
    TableColumn<Pair<String, String>, String> attrValueColumn;
    @FXML
    TextField passwordField;
    @FXML
    TextField repeatField;
    @FXML
    MenuItem extractMenuItem;
    private SimpleBooleanProperty hasAttachments = new SimpleBooleanProperty();

    private Node focusNode;
    private TreeItem<JaxbGroupBinding> rootGroup;
    private Map<UUID, javafx.scene.image.ImageView> iconsMap;
    private KeePassFile keePassFile;
    private String username;
    private String password;
    private AtomicBoolean isModified = new AtomicBoolean();
    private List<String> recentFiles = new ArrayList<>();
    private AppPrefs appPrefs;

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
                                } else {
                                    System.out.printf("No icon for: %s\n", item.getName());
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
        attrNameColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getKey()));
        attrValueColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getValue()));
        rootGroup.setExpanded(true);
    }

    public void addRecentFile(String kdbxFile) {
        recentFiles.add(kdbxFile);
        appPrefs.getRecentFiles().add(kdbxFile);
        try {
            JsonDBTemplate dbTemplate = PrefsUtils.getPreferences(AppPrefs.class);
            dbTemplate.upsert(appPrefs);
        } catch (Exception e) {
            Dialogs.showExceptionAlert("AppPrefs", "Failed to update prefs", e);
        }
    }

    public void setFocusNode(Node focusNode) {
        this.focusNode = focusNode;
    }
    @FXML
    private void initialize() {
        System.out.printf("KeePassController.initialize\n");
        menuBar.setUseSystemMenuBar(true);
        recentFilesMenu.setOnShowing(this::showingRecentFiles);
        try {
            JsonDBTemplate dbTemplate = PrefsUtils.getPreferences(AppPrefs.class);
            appPrefs = dbTemplate.findById("KeePassJava2Prefs", AppPrefs.class);
        } catch (Exception e) {
            Dialogs.showExceptionAlert("AppPrefs", "Failed to load AppPrefs", e);
            appPrefs = new AppPrefs();
        }
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
                attachmentList.getItems().clear();
                attachmentList.getItems().addAll(attachments);
                hasAttachments.set(true);
                System.out.printf("Extract should be enabled\n");
            } else {
                attachmentList.getItems().clear();
            }
            Set<String> attrNames = entry.getAttributes();
            List<Pair<String,String>> tmp = new ArrayList<>();
            for(String name : attrNames) {
                String value = entry.getAttributeValue(name);
                Pair<String,String> pair = new Pair<>(name, value);
                tmp.add(pair);
            }
            ObservableList<Pair<String,String>> attrValues = FXCollections.observableArrayList(tmp);
            attributesTable.setItems(attrValues);
        }
    }

    @FXML
    private void showingRecentFiles(Event event) {
        System.out.println(event);
        recentFilesMenu.getItems().clear();
        for(String f : recentFiles) {
            MenuItem menuItem = new MenuItem(f);
            recentFilesMenu.getItems().add(menuItem);
        }
    }

    @FXML
    private void fileOpenRecent() {

    }

    @FXML
    private void editCopy() {
        if(focusNode != null) {
            if(focusNode instanceof TextInputControl) {
                TextInputControl tf = (TextInputControl) focusNode;
                tf.copy();
            }
            else if(focusNode instanceof TableView) {
                TableView tv = (TableView) focusNode;
                Object selected = tv.getSelectionModel().getSelectedItem();
                System.out.printf("TableView.selected: %s\n", selected.getClass());
                if(selected instanceof Pair) {
                    Pair<String, String> p = (Pair<String, String>) selected;
                    if(p.getValue() != null && p.getValue().length() > 0) {
                        final ClipboardContent content = new ClipboardContent();
                        content.putString(p.getValue());
                        Clipboard.getSystemClipboard().setContent(content);
                    }
                }
            }
            else if(focusNode instanceof ListView) {
                ListView lv = (ListView) focusNode;
                Object selected = lv.getSelectionModel().getSelectedItem();
                System.out.printf("ListView.selected: %s\n", selected.getClass());
            }
            else {
                System.err.printf("Don't know how to copy type: %s\n", focusNode);
            }
        }
    }
    @FXML
    private void editPaste() {

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
            System.out.printf("copyPassword, %s\n", value);
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
    private void entryEdit() {
        Dialogs.showWarning("Entry->Edit", "Entry-Edit is not implemented yet");
    }
    @FXML
    private void entryDelete() {
        Dialogs.showWarning("Entry->Delete", "Entry-Delete is not implemented yet");
    }

    @FXML
    private void extractAttachment() {
        KeePassAttachment attachment = attachmentList.getSelectionModel().getSelectedItem();
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
    private void searchKeyPress(KeyEvent keyTyped) {
        String typed = keyTyped.getCharacter();
        TextField searchField = (TextField) keyTyped.getSource();
        String text = searchField.getText();
        System.out.printf("searchKeyPress, typed=%s, src=%s\n", typed, text);
        List<JaxbGroupBinding> groups = this.keePassFile.getRoot().getGroup().getGroup();
        HashSet<KeePassEntry> matches = new HashSet<>();
        for(JaxbGroupBinding group : groups) {
            List<JaxbEntryBinding> bindings = group.getEntry();
            for(JaxbEntryBinding binding : bindings) {
                List<StringField> fields = binding.getString();
                for(StringField field : fields) {
                    if(field.getValue().getValue().contains(text)) {
                        matches.add(new KeePassEntry(group, binding, iconsMap));
                    }
                }
            }
        }
        List<ObservableValue<String>> names = matches.stream().map(KeePassEntry::getTitle).collect(Collectors.toList());
        System.out.printf("matches(%d): %s\n", names.size(), names);
    }
    @FXML
    private void searchAction() {
        System.out.printf("searchAction clicked\n");
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
    @FXML
    private void showContextMenu(EventHandler<MouseEvent> event) {
        System.out.printf("showContextMenu#2, %s", event);
    }
    @FXML
    private void showContextMenu(MouseEvent event) {
        System.out.printf("showContextMenu, %s", event);
    }
}
