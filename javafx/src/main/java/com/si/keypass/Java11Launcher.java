package com.si.keypass;

/**
 * Launcher to workaround fat jar problem under Java 11
 * https://stackoverflow.com/questions/52653836/maven-shade-javafx-runtime-components-are-missing
 */
public class Java11Launcher {
    public static void main(String[] args) {
        KeePassFX2.main(args);
    }
}
