package com.si.keypass;

import javafx.scene.control.TreeCell;
import javafx.scene.image.ImageView;
import org.linguafranca.pwdb.kdbx.jaxb.JaxbGroup;

import java.util.Map;
import java.util.UUID;

public class JaxbGroupTreeCell extends TreeCell<JaxbGroup> {
    private Map<UUID, ImageView> iconsMap;

    public JaxbGroupTreeCell(Map<UUID, ImageView> iconsMap) {
        this.iconsMap = iconsMap;
    }
    @Override
    protected void updateItem(JaxbGroup item, boolean empty) {
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
}
