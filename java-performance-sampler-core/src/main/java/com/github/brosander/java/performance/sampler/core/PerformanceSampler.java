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

package com.github.brosander.java.performance.sampler.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class PerformanceSampler {
    private final Logger logger = LoggerFactory.getLogger(PerformanceSampler.class);

    private final Supplier<Long> delaySupplier;
    private final PerformanceSampleElement performanceSampleElement;
    private final AtomicBoolean stopped;
    private final OutputStreamFactory outputStreamFactory;

    public PerformanceSampler(Supplier<Long> delaySupplier, OutputStreamFactory outputStreamFactory) {
        this.delaySupplier = delaySupplier;
        this.performanceSampleElement = new PerformanceSampleElement();
        this.stopped = new AtomicBoolean(true);
        this.outputStreamFactory = outputStreamFactory;
    }

    public void startSampling() {
        if (!stopped.getAndSet(false)) {
            throw new IllegalStateException("Already sampling");
        }

        Thread thread = new Thread(() -> {
            while (!stopped.get()) {
                long lastRunTime = System.currentTimeMillis();
                Thread.getAllStackTraces().values().stream().map(StackTraceElementsIterator::new).sequential().forEachOrdered(performanceSampleElement::accept);
                try {
                    Thread.sleep(getDelay(delaySupplier, lastRunTime));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            }
        });
        thread.setName("PerformanceSamplerThread-" + UUID.randomUUID().toString());
        thread.start();
    }

    private long getDelay(Supplier<Long> delaySupplier, long lastRunTime) {
        return Math.max(0, getNextScheduledRun(delaySupplier, lastRunTime) - System.currentTimeMillis());
    }

    private long getNextScheduledRun(Supplier<Long> delaySupplier, long lastRunTime) {
        long nextRun = lastRunTime + delaySupplier.get();
        long currentTimeMillis = System.currentTimeMillis();
        if (nextRun < currentTimeMillis && logger.isWarnEnabled()) {
            logger.warn("Next scheduled run " + nextRun + " is less than current time " + currentTimeMillis + ".");
        }
        return nextRun;
    }

    public void stopSampling() throws IOException {
        if (stopped.getAndSet(true)) {
            throw new IllegalStateException("Already stopped");
        }
        try (OutputStream out = outputStreamFactory.create("performance-sample-" + System.currentTimeMillis() + ".json")) {
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(out, performanceSampleElement);
        }
    }
}
