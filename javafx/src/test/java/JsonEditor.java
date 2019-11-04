import com.si.keypass.Dialogs;
import com.si.keypass.KeePassEntry;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.linguafranca.pwdb.kdbx.jaxb.JaxbEntry;
import org.linguafranca.pwdb.kdbx.jaxb.binding.JaxbEntryBinding;
import org.linguafranca.pwdb.kdbx.jaxb.binding.StringField;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.UUID;

public class JsonEditor extends Application {
    @FXML
    private TextArea editArea;
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.printf("JsonEditor.start...\n");

        URL fxml = getClass().getResource("JsonEditor.fxml");
        FXMLLoader loader = new FXMLLoader(fxml);
        Parent root = loader.load();
        Object controller = loader.getController();
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
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
    private void editEntry() throws Exception {
        URL fxml = getClass().getResource("JsonEditEntry.fxml");
        FXMLLoader loader = new FXMLLoader(fxml);
        loader.setController(this);
        Parent parent = loader.load();
        Scene scene = new Scene(parent, 600, 300);
        Stage loadDbStage = new Stage();
        loadDbStage.initModality(Modality.APPLICATION_MODAL);
        loadDbStage.setScene(scene);
        // Write out the template json
        JaxbEntryBinding entryBinding = new JaxbEntryBinding();
        entryBinding.setUUID(UUID.randomUUID());
        entryBinding.getString().add(stringField(JaxbEntry.STANDARD_PROPERTY_NAME_TITLE, "--Title--"));
        entryBinding.getString().add(stringField(JaxbEntry.STANDARD_PROPERTY_NAME_NOTES, "--Notes--"));
        entryBinding.getString().add(stringField(JaxbEntry.STANDARD_PROPERTY_NAME_PASSWORD, "--Password--"));
        entryBinding.getString().add(stringField(JaxbEntry.STANDARD_PROPERTY_NAME_URL, "--URL--"));
        entryBinding.getString().add(stringField(JaxbEntry.STANDARD_PROPERTY_NAME_USER_NAME, "--UserName--"));
        entryBinding.getString().add(stringField("Attribute1", "--Attribute1Value--"));
        JAXBContext jc = JAXBContext.newInstance(JaxbEntryBinding.class);
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        StringWriter writer = new StringWriter();
        marshaller.marshal(entryBinding, writer);
        this.editArea.setText(writer.toString());
        loadDbStage.showAndWait();
        scene.setCursor(Cursor.WAIT);
        // Get the updated
        String updateText = editArea.getText();
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        JaxbEntryBinding updatedBinding = (JaxbEntryBinding) unmarshaller.unmarshal(new StringReader(updateText));
        for (StringField field : updatedBinding.getString()) {
            System.out.printf("%s: %s\n", field.getKey(), field.getValue().getValue());
        }
        scene.setCursor(Cursor.DEFAULT);
    }
}
