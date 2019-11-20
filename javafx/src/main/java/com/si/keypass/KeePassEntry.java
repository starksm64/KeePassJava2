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
import javafx.beans.property.StringProperty;
import javafx.beans.property.adapter.JavaBeanStringPropertyBuilder;
import javafx.beans.value.ObservableValue;
import javafx.scene.image.ImageView;
import org.jetbrains.annotations.NotNull;
import org.linguafranca.pwdb.kdbx.jaxb.JaxbEntry;
import org.linguafranca.pwdb.kdbx.jaxb.JaxbGroup;
import org.linguafranca.pwdb.kdbx.jaxb.binding.JaxbEntryBinding;
import org.linguafranca.pwdb.kdbx.jaxb.binding.StringField;

public class KeePassEntry implements Comparable<KeePassEntry> {
    private JaxbEntry binding;
    private JaxbGroup group;
    private Map<UUID, ImageView> iconsMap = new HashMap<>();
    private ArrayList<KeePassAttachment> attachments = new ArrayList<>();
    private SimpleObjectProperty<ImageView> iconProperty;

    public KeePassEntry(JaxbGroup group, JaxbEntry binding, Map<UUID, ImageView> iconsMap) {
        this.group = group;
        this.binding = binding;
        this.iconsMap = iconsMap;
    }

    public JaxbGroup getGroup() {
        return group;
    }

    public String getPassword() {
        return getField("Password");
    }
    public void setPassword(String password) {
        setField("Password", password);
    }
    public StringProperty passwordProperty() {
        StringProperty property = null;
        try {
            property = JavaBeanStringPropertyBuilder.create()
                    .bean(this)
                    .name("password")
                    .build();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return property;
    }
    public String getUsername() {
        return getField("UserName");
    }
    public void setUsername(String username) {
        setField("UserName", username);
    }
    public String getURL() {
        return getField("URL");
    }
    public void setURL(String url) {
        setField("URL", url);
    }
    public String getNotes() {
        return getField("Notes");
    }
    public void setNotes(String notes) {
        setField("Notes", notes);
    }
    /**
     * These are the fields other than the well known ones
     * @return
     */
    public Set<String> getAttributes() {
        HashSet<String> names = new HashSet<>();
        for(String name : binding.getBinaryPropertyNames()) {
            names.add(name);
        }
        for(String name : binding.getPropertyNames()) {
            names.add(name);
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

    public void addAttribute(String name, String value) {
        setField(name, value);
    }

    public List<KeePassAttachment> getAttachments() {
        return attachments;
    }

    public ObservableValue<ImageView> getIcon() {
        if(iconProperty == null) {
            UUID uuid = binding.getCustomIconUUID();
            ImageView icon = iconsMap.get(uuid);
            if (icon == null) {
                UUID groupUuid = group.getCustomIconUUID();
                System.out.printf("icon(%s), entry icon is null, trying %s\n", getTitle(), groupUuid);
                icon = iconsMap.get(group.getCustomIconUUID());
                icon = new ImageView(icon.getImage());
                uuid = UUID.randomUUID();
                binding.setCustomIconUUID(uuid);
                iconsMap.put(uuid, icon);
            }
            System.out.printf("icon(%s), %s,%s\n", getTitle(), uuid, icon);
            iconProperty = new SimpleObjectProperty<>(icon);
        }
        return iconProperty;
    }
    public void setIcon(UUID uuid) {
        binding.setCustomIconUUID(uuid);
        ImageView icon = iconsMap.get(uuid);
        if (icon == null) {
            UUID groupUuid = group.getCustomIconUUID();
            System.out.printf("icon(%s), entry icon is null, trying %s\n", getTitle(), groupUuid);
            icon = iconsMap.get(group.getCustomIconUUID());
            icon = new ImageView(icon.getImage());
            uuid = UUID.randomUUID();
            binding.setCustomIconUUID(uuid);
            iconsMap.put(uuid, icon);
        }
        iconProperty.setValue(icon);
        System.out.printf("updated icon(%s), %s,%s\n", getTitle(), uuid, icon);
    }

    public StringProperty nameProperty() {
        return getFieldProperty("name");
    }
    public String getName() {
        return getField("UserName");
    }
    public void setName(String name) {
        setField("UserName", name);
    }
    public StringProperty urlProperty() {
        StringProperty property = null;
        try {
            property = JavaBeanStringPropertyBuilder.create()
                    .bean(this)
                    .name("URL")
                    .build();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return property;
    }
    public StringProperty titleProperty() {
        StringProperty titleProperty = null;
        try {
            titleProperty = JavaBeanStringPropertyBuilder.create()
                    .bean(this)
                    .name("title")
                    .build();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return titleProperty;
    }
    public String getTitle() {
        String title = getField("Title");
        return title;
    }
    public void setTitle(String title) {
        setField("Title", title);
        System.out.printf("Title changed to: %s\n", title);
    }

    public JaxbEntryBinding getDelegate() {
        return this.binding.getDelegate();
    }
    public void updateDelegate(JaxbEntryBinding updates) {
        HashMap<String, String> standardFields = new HashMap<>();
        HashMap<String, String> nonstandardFields = new HashMap<>();

        for(StringField field : updates.getString()) {
            if(JaxbEntry.isStandardName(field.getKey())) {
                standardFields.put(field.getKey(), field.getValue().getValue());
            } else {
                nonstandardFields.put(field.getKey(), field.getValue().getValue());
            }
        }
        if(standardFields.containsKey(JaxbEntry.STANDARD_PROPERTY_NAME_TITLE)) {
            titleProperty().setValue(standardFields.get(JaxbEntry.STANDARD_PROPERTY_NAME_TITLE));
        }
        if(standardFields.containsKey(JaxbEntry.STANDARD_PROPERTY_NAME_USER_NAME)) {
            nameProperty().setValue(standardFields.get(JaxbEntry.STANDARD_PROPERTY_NAME_USER_NAME));
        }
        if(standardFields.containsKey(JaxbEntry.STANDARD_PROPERTY_NAME_URL)) {
            urlProperty().setValue(standardFields.get(JaxbEntry.STANDARD_PROPERTY_NAME_URL));
        }
        if(standardFields.containsKey(JaxbEntry.STANDARD_PROPERTY_NAME_PASSWORD)) {
            passwordProperty().setValue(standardFields.get(JaxbEntry.STANDARD_PROPERTY_NAME_PASSWORD));
        }
        if(standardFields.containsKey(JaxbEntry.STANDARD_PROPERTY_NAME_NOTES)) {
            setNotes(standardFields.get(JaxbEntry.STANDARD_PROPERTY_NAME_NOTES));
        }
        // TODO: need to remove any removed non-standard properties
        for(String name : nonstandardFields.keySet()) {
        }
    }
    @Override
    public int compareTo(@NotNull KeePassEntry o) {
        String myTitle = getTitle();
        String oTitle = o.getTitle();
        return myTitle.compareTo(oTitle);
    }

    @Override
    public int hashCode() {
        return getTitle().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        KeePassEntry entry = (KeePassEntry) obj;
        if(entry != null) {
            String title = getTitle();
            String objTitle = entry.getTitle();
            return title.equals(objTitle);
        }
        return false;
    }

    public String toString() {
        StringBuilder tmp = new StringBuilder("KeePassEntry: "+ getTitle());
        tmp.append("\ntags: ");
        tmp.append(binding.getTags());
        tmp.append("\nstrings: ");
        for(String name : binding.getPropertyNames()) {
            tmp.append(String.format("%s/%s\n", name, binding.getProperty(name)));
        }
        tmp.append(String.format("\ncreate: %s, usage: %d", binding.getCreationTime(), binding.getTimes().getUsageCount()));
        tmp.append("\nbinaries: ");
        for(String name : binding.getBinaryPropertyNames()) {
            byte[] data = binding.getBinaryProperty(name);
            if(data == null) {
                data = new byte[0];
            }
            tmp.append(String.format("%s/%d\n", name, data.length));
        }
        tmp.append("\nattributes: "+getAttributes());
        return tmp.toString();
    }

    private StringProperty getFieldProperty(String name) {
        StringProperty property = null;
        try {
            property = JavaBeanStringPropertyBuilder.create()
                    .bean(this)
                    .name(name)
                    .build();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return property;
    }
    private String getField(String name) {
        String fieldValue =  binding.getProperty(name);
        return fieldValue;
    }
    private void setField(String name, String value) {
        binding.setProperty(name, value);
    }
}
