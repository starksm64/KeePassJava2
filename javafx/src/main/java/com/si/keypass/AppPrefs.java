package com.si.keypass;

import java.util.ArrayList;
import java.util.List;

import io.jsondb.annotation.Document;
import io.jsondb.annotation.Id;

@Document(collection = "KeePassJava2Prefs", schemaVersion= "1.0")
public class AppPrefs {
    @Id
    private String id;
    private List<String> recentFiles = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getRecentFiles() {
        return recentFiles;
    }

    public void setRecentFiles(List<String> recentFiles) {
        this.recentFiles = recentFiles;
    }
}
