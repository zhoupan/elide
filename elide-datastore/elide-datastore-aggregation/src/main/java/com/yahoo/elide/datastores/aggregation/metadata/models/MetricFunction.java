/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.datastores.aggregation.metadata.metric.MetricFunctionInvocation;
import com.yahoo.elide.request.Argument;

import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * Functions used to compute metrics.
 */
@Include(type = "metricFunction")
@Entity
@ToString
@Data
public abstract class MetricFunction {
    @Id
    private String name;

    private String longName;

    private String description;

    private String expression;

    @OneToMany
    private Set<FunctionArgument> arguments;

    public MetricFunction(String name, String longName, String description,
                          Set<FunctionArgument> arguments, String expression) {
        this.name = name;
        this.longName = longName;
        this.description = description;
        this.arguments = arguments;
        this.expression = expression;
    }

    public MetricFunction(String name, String longName, String description, String expression) {
        this(name, longName, description, Collections.emptySet(), expression);
    }

    public MetricFunctionInvocation invoke(Map<String, Argument> arguments,
                                           String alias) {
        final MetricFunction function = this;
        return new MetricFunctionInvocation() {
            @Override
            public List<Argument> getArguments() {
                return new ArrayList<>(arguments.values());
            }

            @Override
            public Argument getArgument(String argumentName) {
                return arguments.get(argumentName);
            }

            @Override
            public MetricFunction getFunction() {
                return function;
            }

            @Override
            public String getAlias() {
                return alias;
            }
        };
    }

    /**
     * Get all required argument names for this metric function.
     *
     * @return all argument names
     */
    private Set<String> getArgumentNames() {
        return getArguments().stream().map(FunctionArgument::getName).collect(Collectors.toSet());
    }

    /**
     * Invoke this metric function with arguments, an aggregated field and projection alias.
     *
     * @param arguments arguments provided in the request
     * @param alias result alias
     * @return an invoked metric function
     */
    public final MetricFunctionInvocation invoke(Set<Argument> arguments,
                                                 String alias) {
        Set<String> requiredArguments = getArgumentNames();
        Set<String> providedArguments = arguments.stream()
                .map(Argument::getName)
                .collect(Collectors.toSet());

        if (!requiredArguments.equals(providedArguments)) {
            throw new InvalidPredicateException(
                    "Provided arguments doesn't match requirement for function " + getName() + ".");
        }

        // map arguments to their actual name
        Map<String, Argument> resolvedArguments = arguments.stream()
                .collect(Collectors.toMap(Argument::getName, Function.identity()));

        return invoke(resolvedArguments, alias);
    }
}
