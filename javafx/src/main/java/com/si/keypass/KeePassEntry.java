package com.si.keypass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.jetbrains.annotations.NotNull;
import org.linguafranca.pwdb.kdbx.jaxb.binding.BinaryField;
import org.linguafranca.pwdb.kdbx.jaxb.binding.JaxbEntryBinding;
import org.linguafranca.pwdb.kdbx.jaxb.binding.JaxbGroupBinding;
import org.linguafranca.pwdb.kdbx.jaxb.binding.StringField;

public class KeePassEntry implements Comparable<KeePassEntry> {
    JaxbEntryBinding binding;
    JaxbGroupBinding group;
    Map<UUID, ImageView> iconsMap = new HashMap<>();
    ArrayList<KeePassAttachment> attachments = new ArrayList<>();

    public KeePassEntry(JaxbGroupBinding group, JaxbEntryBinding binding, Map<UUID, ImageView> iconsMap) {
        this.group = group;
        this.binding = binding;
        this.iconsMap = iconsMap;
    }

    public String getPassword() {
        return getField("Password");
    }
    public String getUsername() {
        return getField("UserName");
    }
    public String getURL() {
        return getField("URL");
    }
    public String getNotes() {
        return getField("Notes");
    }

    /**
     * These are the fields other than the well known ones
     * @return
     */
    public Set<String> getAttributes() {
        HashSet<String> names = new HashSet<>();
        for(StringField field : binding.getString()) {
            names.add(field.getKey());
        }
        names.remove("Title");
        names.remove("UserName");
        names.remove("Password");
        names.remove("Repeat");
        names.remove("URL");
        names.remove("Expires");
        names.remove("Notes");
        return names;
    }
    public String getAttributeValue(String name) {
        return getField(name);
    }

    public List<KeePassAttachment> getAttachments() {
        return attachments;
    }

    public ObservableValue<ImageView> getIcon() {
        UUID uuid = binding.getCustomIconUUID();
        ImageView icon = iconsMap.get(uuid);
        if(icon == null) {
            UUID groupUuid = group.getCustomIconUUID();
            System.out.printf("icon(%s), entry icon is null, trying %s\n", getTitle(), groupUuid);
            icon = iconsMap.get(group.getCustomIconUUID());
            icon = new ImageView(icon.getImage());
            uuid = UUID.randomUUID();
            binding.setCustomIconUUID(uuid);
            iconsMap.put(uuid, icon);
        }
        System.out.printf("icon(%s), %s,%s\n", getTitle(), uuid, icon);
        return new SimpleObjectProperty<>(icon);
    }

    public ObservableValue<String> getName() {
        String name = "";
        for(StringField field : binding.getString()) {
            if(field.getKey().equals("UserName")) {
                name = field.getValue().getValue();
            }
        }
        return new ReadOnlyStringWrapper(name);
    }
    public ObservableValue<String> getURLValue() {
        String url = "";
        for(StringField field : binding.getString()) {
            if(field.getKey().equals("URL")) {
                url = field.getValue().getValue();
            }
        }
        return new ReadOnlyStringWrapper(url);
    }
    public ObservableValue<String> getTitle() {
        String title = "";
        for(StringField field : binding.getString()) {
            if(field.getKey().equals("Title")) {
                title = field.getValue().getValue();
            }
        }
        return new ReadOnlyStringWrapper(title);
    }

    @Override
    public int compareTo(@NotNull KeePassEntry o) {
        String myTitle = getTitle().getValue();
        String oTitle = o.getTitle().getValue();
        return myTitle.compareTo(oTitle);
    }

    @Override
    public int hashCode() {
        return getTitle().getValue().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        KeePassEntry entry = (KeePassEntry) obj;
        return getTitle().getValue().equals(entry.getName().getValue());
    }

    public String toString() {
        StringBuilder tmp = new StringBuilder("KeePassEntry: "+getTitle());
        tmp.append("\ntags: ");
        tmp.append(binding.getTags());
        tmp.append("\nstrings: ");
        for(StringField field : binding.getString()) {
            tmp.append(String.format("%s/%s\n", field.getKey(), field.getValue().getValue()));
        }
        tmp.append(String.format("\ncreate: %s, usage: %d", binding.getTimes().getCreationTime(), binding.getTimes().getUsageCount()));
        tmp.append("\nbinaries: ");
        for(BinaryField field : binding.getBinary()) {
            tmp.append(String.format("%s/%s\n", field.getKey(), field.getValue().getRef()));
        }
        tmp.append("\nattributes: "+getAttributes());
        return tmp.toString();
    }

    private String getField(String name) {
        String fieldValue = null;
        for(StringField field : binding.getString()) {
            if(field.getKey().equals(name)) {
                fieldValue = field.getValue().getValue();
            }
        }
        return fieldValue;
    }

}
