/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.dynamodb.models;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import javax.persistence.Entity;

import static com.yahoo.elide.datastores.dynamodb.models.Person.PERSON_TABLE_NAME;

@Entity
@DynamoDBTable(tableName = PERSON_TABLE_NAME)
public class Person {
    public static final String PERSON_TABLE_NAME = "personTable";

    private String id;
    private String name;
    private Long age;

    @DynamoDBHashKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @DynamoDBAttribute
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @DynamoDBAttribute
    public Long getAge() {
        return age;
    }

    public void setAge(Long age) {
        this.age = age;
    }
}
