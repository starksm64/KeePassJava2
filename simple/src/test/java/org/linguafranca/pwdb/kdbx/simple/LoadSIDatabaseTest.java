package org.linguafranca.pwdb.kdbx.simple;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;

import org.junit.Test;
import org.linguafranca.pwdb.Credentials;
import org.linguafranca.pwdb.Visitor;
import org.linguafranca.pwdb.kdbx.KdbxCreds;

/**
 * Created by starksm on 5/28/17.
 */
public class LoadSIDatabaseTest {
    @Test
    public void loadKdbx() throws Exception {
        // get an input stream from KDB file
        FileInputStream kdbxIS = new FileInputStream("/Users/starksm/private/Banking/SIKeyPass.kdbx");
        // Read the password from /tmp/testLoadDB.pass
        FileReader reader = new FileReader("/tmp/testLoadDB.pass");
        BufferedReader br = new BufferedReader(reader);
        String pass = br.readLine();
        br.close();
        Credentials credentials = new KdbxCreds(pass.getBytes());

        SimpleDatabase database = SimpleDatabase.load(credentials, kdbxIS);
        database.visit(new PrintAllVistor());
    }
}
