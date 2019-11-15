/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.metric.functions;

import com.yahoo.elide.datastores.aggregation.metadata.models.MetricFunction;

/**
 * Canned MIN metric function.
 */
public class Min extends MetricFunction {
    public Min() {
        super("Min", "Min", "Calculate min of a metric column");
    }
}
