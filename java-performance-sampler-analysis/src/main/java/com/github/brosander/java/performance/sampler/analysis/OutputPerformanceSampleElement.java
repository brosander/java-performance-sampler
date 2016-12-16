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

package com.github.brosander.java.performance.sampler.analysis;

import com.github.brosander.java.performance.sampler.core.PerformanceSampleElement;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class OutputPerformanceSampleElement extends PerformanceSampleElement {
    public OutputPerformanceSampleElement(PerformanceSampleElement performanceSampleElement) {
        super(performanceSampleElement.getCalls().entrySet()
                .stream().collect(Collectors.toMap(Map.Entry::getKey, e -> new OutputPerformanceSampleElement(e.getValue()))), performanceSampleElement.getSamples());
    }

    @Override
    public Map<String, PerformanceSampleElement> getCalls() {
        return super.getCalls().entrySet().stream().sorted((o1, o2) -> {
            long count1 = o1.getValue().getSamples();
            long count2 = o2.getValue().getSamples();
            if (count1 == count2) {
                return o1.getKey().toString().compareTo(o2.getKey().toString());
            }
            return Long.valueOf(count2).compareTo(Long.valueOf(count1));
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> {
            throw new RuntimeException("Not expecting to merge");
        }, LinkedHashMap::new));
    }
}
