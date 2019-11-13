
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IconGridPane extends Application {
    private Map<UUID, ImageView> iconsMap;
    @FXML
    private Pane rootPane;
    @FXML
    private GridPane gridPane;
    private boolean editCancelled;

    public boolean isEditCancelled() {
        return editCancelled;
    }
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Need an iconCache for testing
        if(iconsMap == null) {
            iconsMap = new HashMap<>();
            RandomAccessFile iconCache = new RandomAccessFile("/tmp/icon.cache", "rw");
            try {
                String id = iconCache.readUTF();
                do {
                    int dataSize = iconCache.readInt();
                    byte[] iconData = new byte[dataSize];
                    iconCache.readFully(iconData);
                    ByteArrayInputStream data = new ByteArrayInputStream(iconData);
                    ImageView iconView = new ImageView(new Image(data, 64, 64, false, false));
                    iconsMap.put(UUID.fromString(id), iconView);
                    id = iconCache.readUTF();
                } while (id != null);
            } catch (EOFException e) {
                // This is ok
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        loadGrid();
        Scene scene = new Scene(rootPane, gridPane.getWidth()+128, gridPane.getHeight());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() {
        System.out.printf("Edit was cancelled: %s\n", isEditCancelled());
    }
    private void loadGrid() {
        gridPane = new GridPane();
        gridPane.setHgap(5);
        gridPane.setVgap(5);
        int COLS = 16;
        Object[] keys = iconsMap.keySet().toArray();
        // Populate grid pane with the custom icons from the DB in 6x* layout
        int count = iconsMap.size();
        int rows = count / COLS;
        if((count % COLS) > 0) {
            rows ++;
        }
        System.out.printf("Processing(%d) icons, %dx%d\n", count, rows, COLS);
        int index = 0;
        for(int row = 0; row < rows; row ++) {
            gridPane.getRowConstraints().add(new RowConstraints(100));
            for(int col = 0; col < COLS && index < count; col ++) {
                if(row == 0) {
                    gridPane.getColumnConstraints().add(new ColumnConstraints(80));
                }
                UUID id = UUID.class.cast(keys[index ++]);
                ImageView icon = iconsMap.get(id);
                Button btn = new Button(id.toString(), icon);
                btn.setPrefSize(64, 64);
                btn.setContentDisplay(ContentDisplay.TOP);
                btn.setOnAction(this::selectedIcon);
                gridPane.add(btn, col, row);
            }
        }
        gridPane.resize(COLS*75, rows*75);
        BorderPane borderPane = new BorderPane();
        borderPane.setPrefSize(COLS*80, 480);
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(gridPane);
        scrollPane.setPannable(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        borderPane.setCenter(scrollPane);
        Button cancelButton = new Button("Cancel");
        cancelButton.setCancelButton(true);
        cancelButton.setDefaultButton(true);
        cancelButton.setOnAction((e) -> cancelEdit(e));
        Button newButton = new Button("New...");
        newButton.setOnAction((e) -> newCustom());
        HBox buttonBox = new HBox();
        buttonBox.getChildren().add(newButton);
        buttonBox.getChildren().add(cancelButton);
        borderPane.setTop(buttonBox);
        this.rootPane = borderPane;
    }

    @FXML
    private void selectedIcon(ActionEvent event) {
        Button btn = (Button) event.getSource();
        System.out.printf("Selected(%s)\n", btn.getText());
    }
    @FXML
    private void newCustom() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Custom Icon");
        fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("PNG", ".png"));
        File selectedFile = fileChooser.showOpenDialog(null);
        if(selectedFile != null) {
            UUID id = UUID.randomUUID();
            try(RandomAccessFile iconFile = new RandomAccessFile(selectedFile, "r")) {
                int length = (int) iconFile.length();
                byte[] iconData = new byte[length];
                iconFile.readFully(iconData);
                ByteArrayInputStream data = new ByteArrayInputStream(iconData);
                ImageView iconView = new ImageView(new Image(data, 64, 64, false, false));
                iconsMap.put(id, iconView);
                int cols = gridPane.getColumnCount();
                int row = (iconsMap.size()-1) / cols;
                int col = (iconsMap.size()-1) % cols;

                Button btn = new Button(id.toString(), iconView);
                btn.setPrefSize(64, 64);
                btn.setContentDisplay(ContentDisplay.TOP);
                btn.setOnAction(this::selectedIcon);
                gridPane.add(btn, col, row);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    @FXML
    private void cancelEdit(ActionEvent event) {
        editCancelled = true;
        Button button = (Button) event.getSource();
        Stage stage = (Stage) button.getScene().getWindow();
        stage.close();
    }
}
