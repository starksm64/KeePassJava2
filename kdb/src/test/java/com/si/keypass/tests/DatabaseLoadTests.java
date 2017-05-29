package com.si.keypass.tests;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;
import org.linguafranca.pwdb.Credentials;
import org.linguafranca.pwdb.kdb4.KdbCredentials;
import org.linguafranca.pwdb.kdb4.KdbDatabase;
import org.linguafranca.pwdb.kdb4.KdbEntry;
import org.linguafranca.pwdb.kdb4.KdbGroup;

/**
 * Created by starksm on 5/28/17.
 */
public class DatabaseLoadTests {
    @Test
    public void testLoadDB() throws IOException {
        // get an input stream from KDB file
        FileInputStream kdbIS = new FileInputStream("/Users/starksm/private/Banking/KeyPass-bak.kdbx");
        // Read the password from /tmp/testLoadDB.pass
        FileReader reader = new FileReader("/tmp/testLoadDB.pass");
        BufferedReader br = new BufferedReader(reader);
        String pass = br.readLine();
        br.close();
        Credentials credentials = new KdbCredentials.Password(pass.getBytes());
        // open database.
        KdbDatabase database = KdbDatabase.load(credentials, kdbIS);
        kdbIS.close();

        System.out.printf("Description: %s\n", database.getDescription());
        System.out.printf("Name: %s\n", database.getName());
        KdbGroup rootGroup = database.getRootGroup();
        for(KdbEntry entry : rootGroup.getEntries()) {
            System.out.printf("\tTitle: %s; %s", entry.getTitle(), entry.getUsername());
        }
    }
}
