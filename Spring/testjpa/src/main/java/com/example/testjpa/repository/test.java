package com.example.testjpa.repository;

import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class test {
    public static void main(String[] args) {
        Map<String,Set<String>> databaseActionMap = null;
        for ( int i=0;i<10;i++ ) {
            final Set<String> contributors;
            if (databaseActionMap == null) {
                contributors = new HashSet<>();
                databaseActionMap = new HashMap<>();
            } else {
                String name = "ko" + i;
                contributors = databaseActionMap.computeIfAbsent(
                        "ko",
                        string -> new HashSet<>()
                );
            }
            System.out.println(contributors.toString());
            contributors.add("grouping.contributor" + i);

        }
    }
}
