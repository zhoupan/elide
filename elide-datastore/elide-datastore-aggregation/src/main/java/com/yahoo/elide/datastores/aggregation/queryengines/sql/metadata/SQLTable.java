/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import com.yahoo.elide.core.exceptions.InternalServerErrorException;
import com.yahoo.elide.datastores.aggregation.AggregationDictionary;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * SQL extension of {@link Table} which also contains sql column meta data.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SQLTable extends Table {
    private Set<SQLColumn> sqlColumns;

    public SQLTable(Class<?> cls, AggregationDictionary dictionary) {
        super(cls, dictionary);
        this.sqlColumns = resolveSQLDimensions(cls, dictionary);
    }

   /**
     * Get sql column meta data based on field name.
     *
     * @param fieldName field name
     * @return sql column
     */
    public SQLColumn getSQLColumn(String fieldName) {
        return getSqlColumns().stream()
                .filter(col -> col.getName().equals(fieldName))
                .findFirst()
                .orElseThrow(() -> new InternalServerErrorException("SQLField not found: " + fieldName));
    }

    /**
     * Resolve all sql columns of a table.
     *
     * @param cls table class
     * @param dictionary dictionary contains the table class
     * @return all resolved sql column metadata
     */
    static Set<SQLColumn> resolveSQLDimensions(Class<?> cls, AggregationDictionary dictionary) {
        return dictionary.getAllFields(cls).stream()
                .filter(field -> !dictionary.isMetricField(cls, field))
                .map(field -> new SQLColumn(cls, field, dictionary))
                .collect(Collectors.toSet());
    }
}
