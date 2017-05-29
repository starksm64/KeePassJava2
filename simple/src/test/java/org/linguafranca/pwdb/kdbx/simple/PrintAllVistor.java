package org.linguafranca.pwdb.kdbx.simple;

import org.linguafranca.pwdb.Entry;
import org.linguafranca.pwdb.Group;
import org.linguafranca.pwdb.Visitor;

public class PrintAllVistor implements Visitor {
    @Override
    public void startVisit(Group group) {
        System.out.printf("Begin Group(%s):\n", group.getName());
    }

    @Override
    public void endVisit(Group group) {
        System.out.printf("End Group(%s):\n", group.getName());

    }

    @Override
    public void visit(Entry entry) {
        System.out.printf("...Begin Entry(%s):\n", entry.getUsername());
        System.out.printf("......UUID: %s\n", entry.getUuid());
        System.out.printf("......URL: %s\n", entry.getUrl());
        if(entry.getBinaryPropertyNames().size() > 0)
            System.out.printf("......bin-properties: %s\n", entry.getBinaryPropertyNames());
    }

    @Override
    public boolean isEntriesFirst() {
        return false;
    }
}
