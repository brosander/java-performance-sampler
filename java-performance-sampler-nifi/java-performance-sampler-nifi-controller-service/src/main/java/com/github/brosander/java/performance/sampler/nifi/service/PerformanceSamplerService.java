/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.brosander.java.performance.sampler.nifi.service;

import com.github.brosander.java.performance.sampler.core.PerformanceSampler;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnDisabled;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.reporting.InitializationException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Tags({"benchmark", "performance"})
@CapabilityDescription("Provides a controller service that collects metrics while running and writes them out to json when stopped.")
public class PerformanceSamplerService extends AbstractControllerService {
    public static final PropertyDescriptor BENCHMARKS_DIRECTORY = new PropertyDescriptor.Builder()
            .name("Benchmarks Directory")
            .description("Directory to write out benchmark json files.")
            .defaultValue("./benchmarks")
            .required(true)
            .expressionLanguageSupported(true)
            .addValidator(new StandardValidators.DirectoryExistsValidator(true, true))
            .build();

    public static final PropertyDescriptor STACK_DUMP_INTERVAL = new PropertyDescriptor.Builder()
            .name("Stack Interval")
            .description("Frequency to do a stack dump")
            .defaultValue("50 ms")
            .required(true)
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.TIME_PERIOD_VALIDATOR)
            .build();

    private PerformanceSampler performanceSampler;

    @OnEnabled
    public void onConfigured(final ConfigurationContext context) throws InitializationException {
        long stackDumpInterval = context.getProperty(STACK_DUMP_INTERVAL).evaluateAttributeExpressions().asTimePeriod(TimeUnit.MILLISECONDS);
        Supplier<Long> delaySupplier = () -> stackDumpInterval;
        File benchmarksDirectory = new File(context.getProperty(BENCHMARKS_DIRECTORY).evaluateAttributeExpressions().getValue());

        performanceSampler = new PerformanceSampler(delaySupplier, name -> new FileOutputStream(new File(benchmarksDirectory, name)));
        performanceSampler.startSampling();
    }

    @OnDisabled
    public void shutdown() {
        try {
            performanceSampler.stopSampling();
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        performanceSampler = null;
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return Arrays.asList(BENCHMARKS_DIRECTORY, STACK_DUMP_INTERVAL);
    }
}

