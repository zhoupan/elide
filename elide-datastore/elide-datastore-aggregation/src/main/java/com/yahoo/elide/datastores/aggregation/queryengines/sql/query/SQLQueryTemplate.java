/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import lombok.Data;

/**
 * SQLQueryTemplate contains projections information about a sql query.
 */
@Data
public class SQLQueryTemplate {
    public SQLQueryTemplate(String queryTemplate) {
        this.queryTemplate = queryTemplate;
    }

    String queryTemplate;
}
