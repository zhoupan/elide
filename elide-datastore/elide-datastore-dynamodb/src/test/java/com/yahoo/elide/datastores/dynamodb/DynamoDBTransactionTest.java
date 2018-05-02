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
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.google.common.collect.ImmutableMap;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.datastores.dynamodb.models.Person;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import static com.yahoo.elide.datastores.dynamodb.models.Person.PERSON_TABLE_NAME;

public class DynamoDBTransactionTest {
    AmazonDynamoDB client;
    DynamoDBProxyServer local;
    DynamoDBTransaction tx;
    RequestScope requestScope;

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
                .withTableName(PERSON_TABLE_NAME)
                .withKeySchema(
                        new KeySchemaElement().withAttributeName("id").withKeyType(KeyType.HASH)
                ).withAttributeDefinitions(
                        new AttributeDefinition("id", ScalarAttributeType.S),
                        new AttributeDefinition("name", ScalarAttributeType.S),
                        new AttributeDefinition("age", ScalarAttributeType.N)
                ).withProvisionedThroughput(new ProvisionedThroughput(5L, 5L))
                .withGlobalSecondaryIndexes(
                        new GlobalSecondaryIndex().withIndexName("secondary").withKeySchema(
                                new KeySchemaElement().withAttributeName("age").withKeyType(KeyType.HASH),
                                new KeySchemaElement().withAttributeName("name").withKeyType(KeyType.RANGE)
                        ).withProvisionedThroughput(new ProvisionedThroughput(5L, 5L))
                        .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
                )
        );
        tx = new DynamoDBTransaction(client);
        requestScope = Mockito.mock(RequestScope.class);
        EntityDictionary dictionary = new EntityDictionary(Collections.emptyMap());
        dictionary.bindEntity(Person.class);
        Mockito.when(requestScope.getDictionary()).thenReturn(dictionary);
    }

    @AfterTest
    public void tearDown() throws Exception {
        local.stop();
    }

    @Test
    public void testSave() {
        Person person = new Person();
        person.setId("abc123");
        person.setName("Example Person");
        person.setAge(26L);

        tx.save(person, requestScope);
        tx.commit(requestScope);

        // Verify it was stored
        Map<String, AttributeValue> item = fetchPersonById("abc123");
        Assert.assertFalse(item.isEmpty());
        Assert.assertEquals(item.get("name").getS(), "Example Person");
        Assert.assertEquals(item.get("age").getN(), "26");
    }

    @Test
    public void testDelete() {
        putPerson("deletePerson", "the person");
        Map<String, AttributeValue> person = fetchPersonById("deletePerson");

        // Ensure storage worked properly
        Assert.assertFalse(person.isEmpty());
        Assert.assertEquals(person.get("name").getS(), "the person");

        // Delete the user through tx interface
        Person personObject = new Person();
        personObject.setId("deletePerson");
        tx.delete(personObject, requestScope);
        tx.commit(requestScope);

        // Ensure fetch returns no user after delete
        person = fetchPersonById("deletePerson");
        Assert.assertNull(person);
    }

    @Test
    public void testFlush() {
        // Should do nothing. Ensure it doesn't blow up.
        tx.flush(requestScope);
    }

    @Test
    public void testLoadObject() {
        putPerson("testSingleLoad", "my user");

        Person person = (Person) tx.loadObject(Person.class, "testSingleLoad", Optional.empty(), requestScope);
        Assert.assertNotNull(person);
        Assert.assertEquals(person.getId(), "testSingleLoad");
        Assert.assertEquals(person.getName(), "my user");
    }

    // NOTE: This test runs before everything so we can _reliably_ query all elements of a type w/o filtering
    @Test(priority = -1)
    public void testLoadObjects() {
        putPerson("multiLoad1", "multiuser 1");
        putPerson("multiLoad2", "multiuser 2");

        Iterator<Person> people = (Iterator)
                tx.loadObjects(Person.class, Optional.empty(), Optional.empty(), Optional.empty(), requestScope)
                .iterator();

        Assert.assertTrue(people.hasNext());

        Person person1 = people.next();
        Assert.assertEquals(person1.getId(), "multiLoad1");
        Assert.assertEquals(person1.getName(), "multiuser 1");

        Assert.assertTrue(people.hasNext());

        Person person2 = people.next();
        Assert.assertEquals(person2.getId(), "multiLoad2");
        Assert.assertEquals(person2.getName(), "multiuser 2");

        Assert.assertFalse(people.hasNext());
    }

    @Test
    public void testClose() throws Exception {
        // Should do nothing. Ensure it doesn't blow up.
        tx.close();
    }

    private void putPerson(String id, String name) {
        client.putItem(new PutItemRequest()
                .withTableName(PERSON_TABLE_NAME)
                .withItem(
                        ImmutableMap.of(
                                "id", new AttributeValue(id),
                                "name", new AttributeValue(name)
                        )
                )
        );
    }

    private Map<String, AttributeValue> fetchPersonById(String id) {
        return client.getItem(PERSON_TABLE_NAME, ImmutableMap.of("id", new AttributeValue(id))).getItem();
    }
}