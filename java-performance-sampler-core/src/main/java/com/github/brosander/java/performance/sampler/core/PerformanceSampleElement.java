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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

public class PerformanceSampleElement implements Consumer<Iterator<String>> {
    private Map<String, PerformanceSampleElement> calls = new HashMap<>();
    private long samples = 0;

    public PerformanceSampleElement() {
    }

    public PerformanceSampleElement(Map<String, PerformanceSampleElement> calls, long samples) {
        this.calls = calls;
        this.samples = samples;
    }

    @Override
    public void accept(Iterator<String> stackElements) {
        if (stackElements.hasNext()) {
            calls.computeIfAbsent(stackElements.next(), s -> new PerformanceSampleElement()).accept(stackElements);
        } else {
            samples++;
        }
    }

    public Map<String, PerformanceSampleElement> getCalls() {
        return calls;
    }

    public void setCalls(Map<String, PerformanceSampleElement> calls) {
        this.calls = calls;
    }

    public long getSamples() {
        return samples;
    }

    public void setSamples(long samples) {
        this.samples = samples;
    }
}
