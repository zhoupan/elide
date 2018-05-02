/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DynamoDBDataStore implements DataStore {
    private final AmazonDynamoDB dynamoDB;
    private final List<String> packages;

    public DynamoDBDataStore(AmazonDynamoDB dynamoDB, String... packages) {
        this(dynamoDB, Arrays.asList(packages));
    }

    public DynamoDBDataStore(AmazonDynamoDB dynamoDB, List<String> packages) {
        this.dynamoDB = dynamoDB;
        this.packages = Collections.unmodifiableList(packages);
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        // TODO: Find things in package I suppose... Look in all packages
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        return new DyanmoDBTransaction(dynamoDB);
    }

    @Override
    public DataStoreTransaction beginReadTransaction() {
        // TODO: Implement RO form.
        return beginTransaction();
    }
}
