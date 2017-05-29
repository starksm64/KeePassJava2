/*
 * Copyright 2015 Jo Rabin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.linguafranca.pwdb.checks;

import org.junit.Assert;
import org.junit.Test;
import org.linguafranca.pwdb.*;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author jo
 */
public abstract class DatabaseLoaderChecks <D extends Database<D,G,E,I>, G extends Group<D,G,E,I>, E extends Entry<D,G,E,I>, I extends Icon>{
    protected Database<D,G,E,I> database;

    /**
     * a test123 file for each format. Should contain the same thing. This is a basic sanity check.
     */
    @Test
    public void test123File() {

        // visit all groups and entries and list them to console
        database.visit(new Visitor.Print());

        // find all entries in the database
        // the kdb4 version has three additional system related entries
        List<? extends E> anything = database.findEntries("");
        Assert.assertTrue(10 <= anything.size());

        // find all entries in the database that have the string "test" in them
        List<? extends E> tests = database.findEntries("test");
        for (Entry tes: tests) {
            System.out.println(tes.getTitle());
        }
        Assert.assertEquals(4, tests.size());
        if (tests.size() > 0) {
            // copy the password of the first entry to the clipboard
            String pass = tests.get(0).getPassword();
/*
            StringSelection selection = new StringSelection(pass);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            System.out.println(pass + " copied to clip board");
*/
            // all the relevant entries should have the password 123
            String pass2 = tests.get(0).getPassword();
            Assert.assertEquals(pass, pass2);
            Assert.assertEquals("123", pass2);
        }

        List<? extends E> passwords = database.findEntries("password");
        Assert.assertEquals(4, passwords.size());
        for (Entry passwordEntry : passwords) {
            assertEquals(passwordEntry.getTitle(), passwordEntry.getPassword());
            System.out.println(passwordEntry.getTitle());
        }

        List<? extends E> entries = database.findEntries(new Entry.Matcher() {
            @Override
            public boolean matches(Entry entry) {
                return entry.getTitle().equals("hello world");
            }});

        Assert.assertEquals(1, entries.size());
        assertEquals("pass", entries.get(0).getPassword());
    }
}
