package com.si.keypass;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/**
 * Utility methods for displaying dialogs
 */
public class Dialogs {
    /** The Ok/Yes/Save type */
    static ButtonType SAVE_TYPE = new ButtonType("Save");
    /** The No/Ignore type */
    static ButtonType IGNORE_TYPE = new ButtonType("Ignore");
    /** The Cancel type */
    static ButtonType CANCEL_TYPE = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

    /**
     * Show an alert with an exception stack trace option
     * @param title - title
     * @param contentText - content title
     * @param ex - exception to display
     */
    public static void showExceptionAlert(String title, String contentText, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(ex.getClass().getSimpleName()+" Dialog");
        alert.setContentText(contentText);

// Create expandable Exception.
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("The exception stacktrace was:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

// Set expandable Exception into the dialog pane.
        alert.getDialogPane().setExpandableContent(expContent);

        alert.showAndWait();
    }

    /**
     * Show an information dialog with an expandable text msg
     * @param title - title
     * @param contentText - content title
     * @param fullText - expandable info text
     */
    public static void showInformation(String title, String contentText, String fullText) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(contentText);

        Label label = new Label("Full Information:");

        TextArea textArea = new TextArea(fullText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);
        alert.getDialogPane().setExpandableContent(expContent);
        alert.showAndWait();
    }

    /**
     * Show a warning dialog
     * @param title - title
     * @param contentText - content title
     */
    public static void showWarning(String title, String contentText) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        //alert.setHeaderText("Look, a Warning Dialog");
        alert.setContentText(contentText);

        alert.showAndWait();
    }

    /**
     * Show a Yes/No/Cancel input dialog
     * @param title - title
     * @param contentText - info about the question being asked
     * @return optional indicating the selection button
     */
    public static Optional<ButtonType> showYesNoCancel(String title, String contentText) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(contentText);

        alert.getButtonTypes().setAll(SAVE_TYPE, IGNORE_TYPE, CANCEL_TYPE);
        Optional<ButtonType> result = alert.showAndWait();
        return result;
    }
}
