package com.si.keypass;

import java.io.*;
import java.net.URL;
import java.rmi.server.ExportException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Pair;
import org.linguafranca.pwdb.kdbx.*;
import org.linguafranca.pwdb.kdbx.jaxb.JaxbDatabase;
import org.linguafranca.pwdb.kdbx.jaxb.JaxbEntry;
import org.linguafranca.pwdb.kdbx.jaxb.JaxbGroup;
import org.linguafranca.pwdb.kdbx.jaxb.JaxbSerializableDatabase;
import org.linguafranca.pwdb.kdbx.jaxb.binding.Binaries;
import org.linguafranca.pwdb.kdbx.jaxb.binding.BinaryField;
import org.linguafranca.pwdb.kdbx.jaxb.binding.CustomIcons;
import org.linguafranca.pwdb.kdbx.jaxb.binding.JaxbEntryBinding;
import org.linguafranca.pwdb.kdbx.jaxb.binding.JaxbGroupBinding;
import org.linguafranca.pwdb.kdbx.jaxb.binding.KeePassFile;
import org.linguafranca.pwdb.kdbx.jaxb.binding.StringField;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import static com.si.keypass.Dialogs.IGNORE_TYPE;
import static com.si.keypass.Dialogs.SAVE_TYPE;

public class KeePassController {
    @FXML
    MenuBar menuBar;
    @FXML
    Menu recentFilesMenu;
    @FXML
    TreeView<JaxbGroup> groupTreeView;
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
    @FXML
    TextArea editArea;
    private SimpleBooleanProperty hasAttachments = new SimpleBooleanProperty();

    private Node focusNode;
    private JaxbDatabase keyPassDB;
    private TreeItem<JaxbGroup> rootGroup;
    private Map<UUID, javafx.scene.image.ImageView> iconsMap;
    private List<CustomIcons.Icon> icons;
    // Backup of the db file created on open
    private File keePassFileBackup;
    private CompositeKey credentials;
    private KeePassEntry selectedEntry;
    private String username;
    private String password;
    private AtomicBoolean isModified = new AtomicBoolean();
    private List<String> recentFiles = new ArrayList<>();
    private AppPrefs appPrefs;
    private boolean editCancelled;

    public void setRoot(JaxbDatabase keyPassDB, TreeItem<JaxbGroup> rootGroup, Map<UUID, ImageView> iconsMap,
                        List<CustomIcons.Icon> icons, CompositeKey credentials) {
        this.keyPassDB = keyPassDB;
        this.rootGroup = rootGroup;
        this.iconsMap = iconsMap;
        this.icons = icons;
        this.credentials = credentials;
        groupTreeView.setRoot(rootGroup);
        groupTreeView.setCellFactory((x) -> new JaxbGroupTreeCell(iconsMap));
        groupTreeView.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> groupSelected(newValue.getValue()));

        entryTableView.setItems(FXCollections.observableArrayList());
        imageColumn.setCellValueFactory(cdf -> cdf.getValue().getIcon());
        titleColumn.setCellValueFactory(cdf -> cdf.getValue().titleProperty());
        usernameColumn.setCellValueFactory(cdf -> cdf.getValue().nameProperty());
        urlColumn.setCellValueFactory(cdf -> cdf.getValue().urlProperty());
        entryTableView.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> entrySelected(newValue));
        extractMenuItem.disableProperty().bind(hasAttachments.not());
        attrNameColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getKey()));
        attrValueColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getValue()));
        rootGroup.setExpanded(true);
    }

    public void setBackupFile(File backup) {
        this.keePassFileBackup = backup;
    }

    public void addRecentFile(String kdbxFile) {
        if(!recentFiles.contains(kdbxFile)) {
            recentFiles.add(kdbxFile);
            if(!appPrefs.getRecentFiles().contains(kdbxFile)) {
                appPrefs.getRecentFiles().add(kdbxFile);
            }
        }

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

    private void groupSelected(JaxbGroup group) {
        System.out.printf("groupSelected: %s\n", group.getName());
        List<JaxbEntry> bindings = group.getEntries();
        ArrayList<KeePassEntry> tmp = new ArrayList<>();
        for(JaxbEntry binding : bindings) {
            KeePassEntry entry = new KeePassEntry(group, binding, iconsMap);
            for(String name : binding.getBinaryPropertyNames()) {
                byte[] data = binding.getBinaryProperty(name);
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
            this.selectedEntry = entry;
            String notes = entry.getNotes();
            notesArea.setText(notes);
            passwordField.setText(entry.getPassword());
            repeatField.clear();
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
    private void cellEdited(TableColumn.CellEditEvent event) {

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
    private static StringField stringField(String key, String value) {
        StringField field = new StringField();
        field.setKey(key);
        StringField.Value svalue = new StringField.Value();
        svalue.setValue(value);
        field.setValue(svalue);
        return field;
    }

    @FXML
    private void entryNew() {
        JaxbEntry jaxbEntry = keyPassDB.newEntry();
        if(selectedEntry != null) {
            JaxbGroup group = selectedEntry.getGroup();
            group.addEntry(jaxbEntry);
            System.out.printf("1:Adding newly created entry: %s to: %s\n", jaxbEntry.getUuid(), group.getName());
        } else {
            TreeItem<JaxbGroup> group = groupTreeView.getSelectionModel().getSelectedItem();
            if(group != null) {
                group.getValue().addEntry(jaxbEntry);
                System.out.printf("2:Adding newly created entry: %s to: %s\n", jaxbEntry.getUuid(), group.getValue().getName());
            } else {
                keyPassDB.deleteEntry(jaxbEntry.getUuid());
                Dialogs.showWarning("No Group", "No group selected for entry");
                return;
            }
        }
        // Populate with default values
        selectedEntry = new KeePassEntry(jaxbEntry.getParent(), jaxbEntry, iconsMap);
        selectedEntry.setTitle("NewEntryTitle");
        selectedEntry.setPassword("CHANGEME");
        selectedEntry.setUsername("CHANGEME");
        selectedEntry.setNotes("CHANGEME");
        selectedEntry.setURL("CHANGEME");
        selectedEntry.addAttribute("PlaceHolderName", "PlaceHolderValue");
        entryEdit();
        if(editCancelled) {
            System.out.printf("Deleting newly created entry: %s\n", jaxbEntry.getUuid());
            keyPassDB.deleteEntry(jaxbEntry.getUuid());
        }
    }
    /**
     * Display a JSON view of the object for editing in a text area for now.
     */
    @FXML
    private void entryEdit() {
        try {
            editCancelled = false;
            URL fxml = getClass().getResource("/editentry.fxml");
            FXMLLoader loader = new FXMLLoader(fxml);
            loader.setController(this);
            Parent parent = loader.load();
            Scene scene = new Scene(parent, 600, 300);
            Stage loadDbStage = new Stage();
            loadDbStage.initModality(Modality.APPLICATION_MODAL);
            loadDbStage.setScene(scene);
            // Write out the template json
            JaxbEntryBinding entryBinding = selectedEntry.getDelegate();
            JAXBContext jc = JAXBContext.newInstance(JaxbEntryBinding.class);
            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            StringWriter writer = new StringWriter();
            marshaller.marshal(entryBinding, writer);
            this.editArea.setText(writer.toString());
            loadDbStage.showAndWait();
            scene.setCursor(Cursor.WAIT);
        } catch (Exception e) {
            Dialogs.showExceptionAlert("Edit Entry", "Editing failed", e);
        }
    }
    @FXML
    private void saveEdit(ActionEvent event) {
        Button button = (Button) event.getSource();
        Stage stage = (Stage) button.getScene().getWindow();
        stage.close();
        try {
            // Get the updated
            String updateText = editArea.getText();
            JAXBContext jc = JAXBContext.newInstance(JaxbEntryBinding.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            JaxbEntryBinding updatedBinding = (JaxbEntryBinding) unmarshaller.unmarshal(new StringReader(updateText));
            selectedEntry.updateDelegate(updatedBinding);
        } catch (Exception e) {
            Dialogs.showExceptionAlert("Save Entry", "Editing failed", e);
        }
    }
    @FXML
    private void cancelEdit(ActionEvent event) {
        editCancelled = true;
        Button button = (Button) event.getSource();
        Stage stage = (Stage) button.getScene().getWindow();
        stage.close();
    }
    @FXML
    private void entryDelete() {
        if(selectedEntry != null) {
           Optional<ButtonType> btn = Dialogs.showYesNoCancel("Delete entry", "Delete the entry: "+selectedEntry.getTitle());
           if(btn.isPresent() && btn.get() == SAVE_TYPE) {
               UUID uuid = selectedEntry.getDelegate().getUUID();
               boolean deleted = keyPassDB.deleteEntry(uuid);
               if (!deleted) {
                   String msg = String.format("Failed to delete: %s/%s\n", selectedEntry.getTitle(), uuid);
                   System.out.println(msg);
                   Dialogs.showWarning("Failed to delete entry", msg);
               } else {
                   System.out.printf("Deleted %s\n", selectedEntry.getTitle());
                   entryTableView.getItems().remove(selectedEntry);
               }
           }
        } else {
            // A beep character
            System.out.print("\007");
            System.out.flush();
        }
    }
    @FXML
    private void entryIcon() {
        IconGridPane iconGridPane = new IconGridPane();
        try {
            // Update the selected item icon
            UUID currentID = selectedEntry.getDelegate().getCustomIconUUID();
            UUID iconID = iconGridPane.selectIcon(iconsMap, icons, currentID);
            if(iconID != null && !iconID.equals(currentID)) {
                selectedEntry.setIcon(iconID);
                System.out.printf("Selected icon: %s\n", iconID);
            } else {
                System.out.printf("Icon not updated: %s\n", iconID);
            }
        } catch (Exception e) {
            Dialogs.showExceptionAlert("Select Icon", "", e);
        }
    }

    /**
     * The Save button action that validates that the Password/Repeat fields match and
     * then updates the selectedEntry password.
     */
    @FXML
    private void updatePassword() {
        String pass = passwordField.getText();
        String check = repeatField.getText();
        if(pass.compareTo(check) == 0) {
            System.out.printf("updated password for: %s\n", selectedEntry.getTitle());
            selectedEntry.setPassword(pass);
            repeatField.clear();
        } else {
            Dialogs.showWarning("SavePassword", "Passwords do not match");
        }
    }

    /**
     * The Random button action that populates the Password field with a random value.
     */
    @FXML
    private void randomPassword() {
        try {
            List<CharacterRule> rules = Arrays.asList(
                    // at least 2 upper-case character
                    new CharacterRule(EnglishCharacterData.UpperCase, 2),
                    // at least 2 lower-case character
                    new CharacterRule(EnglishCharacterData.LowerCase, 2),
                    // at least one lower-case character
                    new CharacterRule(MySpecialChars.INSTANCE, 1),
                    // at least one digit character
                    new CharacterRule(EnglishCharacterData.Digit, 1));
            PasswordGenerator generator = new PasswordGenerator();
            String password = generator.generatePassword(16, rules);
            passwordField.setText(password);
        } catch (Exception e) {
            Dialogs.showExceptionAlert("PasswordGenerator Failure", "Failed to generate random password", e);
        }
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
    private void addAttachment() {
        FileChooser fileChooser = new FileChooser();
        List<File> files = fileChooser.showOpenMultipleDialog(null);
        if(files != null) {
            ArrayList<KeePassAttachment> attachments = new ArrayList<>();
            for(File file : files) {
                try(RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                    byte[] data = new byte[(int)raf.length()];
                    raf.readFully(data);
                    KeePassAttachment attachment = new KeePassAttachment(file.getName(), data);
                    attachments.add(attachment);
                } catch (IOException e) {
                    Dialogs.showExceptionAlert("Add Attachment", file.getAbsolutePath(), e);
                }
            }
            selectedEntry.getAttachments().addAll(attachments);
        }
    }
    @FXML
    private void deleteAttachment() {
        KeePassAttachment attachment = attachmentList.getSelectionModel().getSelectedItem();
        if(attachment != null) {
            selectedEntry.getAttachments().remove(attachment);
        }
    }
    @FXML
    private void searchKeyPress(KeyEvent keyTyped) {
        String typed = keyTyped.getCharacter();
        TextField searchField = (TextField) keyTyped.getSource();
        String text = searchField.getText();
        System.out.printf("searchKeyPress, typed=%s, src=%s\n", typed, text);
        List<JaxbGroup> groups = this.keyPassDB.getRootGroup().getGroups();
        HashSet<KeePassEntry> matches = new HashSet<>();
        for(JaxbGroup group : groups) {
           List<JaxbEntry> bindings = group.getEntries();
            for(JaxbEntry binding : bindings) {
                for(String name : binding.getPropertyNames()) {
                    String value = binding.getProperty(name);
                    if(value.contains(text)) {
                        matches.add(new KeePassEntry(group, binding, iconsMap));
                    }
                }
            }
        }
        List<String> names = matches.stream().map(KeePassEntry::getTitle).collect(Collectors.toList());
        System.out.printf("matches(%d): %s\n", names.size(), names);
    }
    @FXML
    private void searchAction() {
        System.out.printf("searchAction clicked\n");
    }

    @FXML
    private void fileSave() {
        try {
            saveChanges();
        } catch (IOException e) {
            Dialogs.showExceptionAlert("Failed to save", "", e);
        }
    }
    @FXML
    private void fileSaveAs() {
    }
    @FXML
    private void fileQuit() {
        // TODO: check for modifications
        if(isModified.get()) {
            Optional<ButtonType> result = Dialogs.showYesNoCancel("Save DB", "DB is");
            if (result.get() == SAVE_TYPE){
                fileSave();
            } else if (result.get() == IGNORE_TYPE) {
                // ... user chose ignore changes
            } else {
                // Cancel the app quit
                return;
            }
        }
        // Check for a backup file
        if(keePassFileBackup != null) {
            Optional<ButtonType> result = Dialogs.showYesNoCancel("Delete backup?", "Should the DB backup be removed?");
            if(result.isPresent() && result.get() == SAVE_TYPE) {
                keePassFileBackup.deleteOnExit();
            }
        }
        Platform.exit();
    }
    @FXML
    private void helpAbout() {
        StringBuilder info = new StringBuilder();
        info.append(String.format("FreeMemory: %s\n", Runtime.getRuntime().freeMemory()));
        info.append("--- SystemProperties:\n");
        for(String name : System.getProperties().stringPropertyNames()) {
            info.append(String.format("%s: %s\n", name, System.getProperty(name)));
        }
        Dialogs.showInformation("About KeyPassFX", "Runtime information", info.toString());
    }
    private void saveChanges() throws IOException {
        FileChooser fc = new FileChooser();
        File saveFile = fc.showSaveDialog(null);
        if(saveFile != null) {
            OutputStream testStream = new FileOutputStream(saveFile);
            keyPassDB.save(credentials, testStream);
            System.out.printf("Saved(%d) to: %s\n", saveFile.length(), saveFile.getAbsolutePath());
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
