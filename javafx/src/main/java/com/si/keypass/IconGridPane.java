package com.si.keypass;

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
import org.linguafranca.pwdb.kdbx.jaxb.binding.CustomIcons;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IconGridPane {
    private Map<UUID, ImageView> iconsMap;
    private Pane rootPane;
    private GridPane gridPane;
    private UUID selectedIcon;
    private List<CustomIcons.Icon> newIcons;
    private boolean editCancelled;

    public boolean isEditCancelled() {
        return editCancelled;
    }
    public UUID selectIcon(Map<UUID, ImageView> iconsMap, List<CustomIcons.Icon> newIcons) throws Exception {
        this.iconsMap = iconsMap;
        this.newIcons = newIcons;
        loadGrid();
        Stage stage = new Stage();
        Scene scene = new Scene(rootPane, gridPane.getWidth()+128, gridPane.getHeight());
        stage.setScene(scene);
        stage.show();
        return selectedIcon;
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
        String id = btn.getText();
        selectedIcon = UUID.fromString(id);
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
                int cols = gridPane.impl_getColumnCount();
                int row = (iconsMap.size()-1) / cols;
                int col = (iconsMap.size()-1) % cols;

                Button btn = new Button(id.toString(), iconView);
                btn.setPrefSize(64, 64);
                btn.setContentDisplay(ContentDisplay.TOP);
                btn.setOnAction(this::selectedIcon);
                gridPane.add(btn, col, row);

                if(newIcons == null) {
                    newIcons = new ArrayList<>();
                }
                CustomIcons.Icon icon = new CustomIcons.Icon();
                icon.setData(iconData);
                icon.setUUID(id);
                newIcons.add(icon);
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
