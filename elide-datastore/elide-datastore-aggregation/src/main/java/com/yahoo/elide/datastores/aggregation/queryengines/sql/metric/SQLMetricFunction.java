/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metric;

import com.yahoo.elide.datastores.aggregation.metadata.metric.MetricFunctionInvocation;
import com.yahoo.elide.datastores.aggregation.metadata.models.FunctionArgument;
import com.yahoo.elide.datastores.aggregation.metadata.models.MetricFunction;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLQueryTemplate;

import java.util.Set;

/**
 * SQL extension of {@link MetricFunction} which would be invoked as sql and can construct sql templates.
 */
public abstract class SQLMetricFunction extends MetricFunction {

    public SQLMetricFunction(String name, String longName, String description,
                             Set<FunctionArgument> arguments, String expression) {
        super(name, longName, description, arguments, expression);
    }


    public SQLQueryTemplate resolve(MetricFunctionInvocation metric) {
        return new SQLQueryTemplate(metric.getFunction().getExpression());
    }
}
