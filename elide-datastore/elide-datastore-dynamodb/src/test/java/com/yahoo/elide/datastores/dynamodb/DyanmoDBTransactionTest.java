/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.dynamodb;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;
import com.amazonaws.services.dynamodbv2.local.server.LocalDynamoDBRequestHandler;
import com.amazonaws.services.dynamodbv2.local.server.LocalDynamoDBServerHandler;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.google.common.collect.ImmutableMap;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

public class DyanmoDBTransactionTest {
    AmazonDynamoDB client;
    DynamoDBProxyServer local;

    @BeforeTest
    public void setup() throws Exception {
        System.setProperty("aws.accessKeyId", "nothing");
        System.setProperty("aws.secretKey", "nothing");
        local = new DynamoDBProxyServer(8001,
                new LocalDynamoDBServerHandler(
                        new LocalDynamoDBRequestHandler(
                                0,
                                true,
                                null,
                                true,
                                false),
                        "*"
                ));
        local.start();
        client = AmazonDynamoDBClientBuilder.standard().withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration("http://localhost:8001", "us-west-2"))
                .build();
        client.createTable(new CreateTableRequest()
                .withTableName("save")
                .withKeySchema(Arrays.asList(
                        new KeySchemaElement().withAttributeName("id").withKeyType(KeyType.HASH))
                ).withAttributeDefinitions(
                        new AttributeDefinition("id", ScalarAttributeType.S)
                ).withProvisionedThroughput(new ProvisionedThroughput(5L, 5L))
        );
    }

    @AfterTest
    public void tearDown() throws Exception {
        local.stop();
    }

    @Test
    public void testSave() {
        client.putItem("save", ImmutableMap.of("id", new AttributeValue("abc123")));
        Map<String, AttributeValue> item = client.getItem("save", Collections.singletonMap("id", new AttributeValue("abc123"))).getItem();
        Assert.assertFalse(item.isEmpty());
        Assert.assertEquals(item.get("id").getS(), "abc123");
    }

    @Test
    public void testDelete() {
    }

    @Test
    public void testFlush() {
    }

    @Test
    public void testCommit() {
    }

    @Test
    public void testCreateObject() {
    }

    @Test
    public void testLoadObject() {
    }

    @Test
    public void testLoadObjects() {
    }

    @Test
    public void testClose() {
    }
}