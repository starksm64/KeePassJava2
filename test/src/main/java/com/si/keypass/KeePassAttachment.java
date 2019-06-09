package com.si.keypass;

public class KeePassAttachment {
    private String name;
    private byte[] data;

    public KeePassAttachment(String name, byte[] data) {
        this.name = name;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public byte[] getData() {
        return data;
    }

    public String toString() {
        return String.format("%s[%d]", name, data.length);
    }
}
