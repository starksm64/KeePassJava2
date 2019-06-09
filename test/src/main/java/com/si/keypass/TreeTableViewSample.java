package com.si.keypass;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.TreeTableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class TreeTableViewSample extends Application {
    private static final Image icon = new Rectangle(12, 12, Color.CORNFLOWERBLUE).snapshot(null, null);

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Tree Table View Samples");
        final Scene scene = new Scene(new Group(), 200, 400);
        Group sceneRoot = (Group)scene.getRoot();

        //Creating tree items
//        final TreeItem<String> childNode1 = new TreeItem<>("Child Node 1", new ImageView(icon));
//        final TreeItem<String> childNode2 = new TreeItem<>("Child Node 2", new ImageView(icon));
//        final TreeItem<String> childNode3 = new TreeItem<>("Child Node 3", new ImageView(icon));

        final TreeItem<String> childNode1 = new TreeItem<>("Child Node 1");
        final TreeItem<String> childNode2 = new TreeItem<>("Child Node 2");
        final TreeItem<String> childNode3 = new TreeItem<>("Child Node 3");

        //Creating the root element
//        final TreeItem<String> root = new TreeItem<>("Root node", new ImageView(icon));
        final TreeItem<String> root = new TreeItem<>("Root node");
        root.setExpanded(true);

        //Adding tree items to the root
        root.getChildren().setAll(childNode1, childNode2, childNode3);

        //Creating a column
        TreeTableColumn<String,String> column = new TreeTableColumn<>("Column");
        column.setPrefWidth(150);
        TreeTableColumn<String,String> title = new TreeTableColumn<>("Title");
        column.setPrefWidth(64);

        //Defining cell content
        column.setCellValueFactory((CellDataFeatures<String, String> p) ->
                                           new ReadOnlyStringWrapper(p.getValue().getValue()));


        // cell factory to display graphic:
        column.setCellFactory(ttc -> new TreeTableCell<String, String>() {

            private final ImageView graphic = new ImageView(icon);

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setGraphic(empty ? null : graphic);
            }
        });

        //Creating a tree table view
        final TreeTableView<String> treeTableView = new TreeTableView<>(root);
        treeTableView.getColumns().add(column);
        treeTableView.setPrefWidth(152);
        treeTableView.setShowRoot(true);
        sceneRoot.getChildren().add(treeTableView);
        stage.setScene(scene);
        stage.show();
    }
}