/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class DyanmoDBTransaction implements DataStoreTransaction {
    private final AmazonDynamoDB dynamoDB;
    private final DynamoDBMapper mapper;
    // TODO: Can we make a batch update request rather than iterating n different operations?
    private final List<Runnable> actions = new ArrayList<>();

    public DyanmoDBTransaction(AmazonDynamoDB dynamoDB) {
        this.dynamoDB = dynamoDB;
        this.mapper = new DynamoDBMapper(dynamoDB);
    }

    @Override
    public void save(Object entity, RequestScope scope) {
        actions.add(() -> mapper.save(entity));
    }

    @Override
    public void delete(Object entity, RequestScope scope) {
        actions.add(() -> mapper.delete(entity));
    }

    @Override
    public void flush(RequestScope scope) {
        // Do nothing.
    }

    @Override
    public void commit(RequestScope scope) {
        actions.forEach(Runnable::run);
    }

    @Override
    public void createObject(Object entity, RequestScope scope) {
        save(entity, scope);
    }

    @Override
    public Object loadObject(Class<?> entityClass,
                             Serializable id,
                             Optional<FilterExpression> filterExpression,
                             RequestScope scope) {
        // TODO: Determine how to denote "relational" things in model. In general, data is likely denormalized,
        // however, if a field maps as an id, a BatchGetItemRequest could help us join "relationships"
        // in a single request
        // TODO: Support for filter expressions.
        return mapper.load(entityClass, id);
    }

    @Override
    public Iterable<Object> loadObjects(
            Class<?> entityClass,
            Optional<FilterExpression> filterExpression,
            Optional<Sorting> sorting,
            Optional<Pagination> pagination,
            RequestScope scope) {
        // TODO: Support for filter expressions.
        // TODO: Support sorting and pagination.
        // TODO: Fetching hierarchy TBD (i.e. "related" objects not denormalized)
        DynamoDBMapperConfig config = DynamoDBMapperConfig.builder().build();
        return mapper.batchLoad(Collections.singletonList(entityClass), config).get(entityClass);
    }

    @Override
    public void close() throws IOException {
        // Do nothing. No real transaction.
    }
}
