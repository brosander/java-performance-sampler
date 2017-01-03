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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brosander.java.performance.sampler.core.PerformanceSampleElement;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class PerformanceSampleAnalyzer {
    public static final String FILE_OPT = "file";
    public static final String OUTPUT_FILE_OPT = "outputFile";
    public static final String DEFAULT_PATTERN = "org\\.apache\\.nifi\\.processor\\.AbstractProcessor\\.onTrigger.*";
    public static final String RELEVANT_PATTERN_OPT = "relevantPattern";

    /**
     * Prints the usage to System.out
     *
     * @param errorMessage optional error message
     * @param options      the options object to print usage for
     */
    public static void printUsageAndExit(String errorMessage, Options options, int exitCode) {
        if (errorMessage != null) {
            System.out.println(errorMessage);
            System.out.println();
            System.out.println();
        }
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.setWidth(160);
        helpFormatter.printHelp("java -jar " + new File(PerformanceSampleAnalyzer.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName(), options);
        System.out.flush();
        System.exit(exitCode);
    }

    public static PerformanceSampleElement relevantElements(Pattern relevantPattern, PerformanceSampleElement performanceSampleElement) {
        Map<String, PerformanceSampleElement> result = new HashMap<>();
        doRelevantElements(relevantPattern, performanceSampleElement, result);
        return new PerformanceSampleElement(result, 0);
    }

    public static void doRelevantElements(Pattern relevantPattern, PerformanceSampleElement performanceSampleElement, Map<String, PerformanceSampleElement> result) {
        for (Map.Entry<String, PerformanceSampleElement> stringElementEntry : performanceSampleElement.getCalls().entrySet()) {
            if (relevantPattern.matcher(stringElementEntry.getKey()).matches()) {
                PerformanceSampleElement existing = result.get(stringElementEntry.getKey());
                if (existing == null) {
                    result.put(stringElementEntry.getKey(), stringElementEntry.getValue());
                } else {
                    merge(existing, stringElementEntry.getValue());
                }
            } else {
                doRelevantElements(relevantPattern, stringElementEntry.getValue(), result);
            }
        }
    }

    public static void updateCounts(PerformanceSampleElement performanceSampleElement) {
        performanceSampleElement.getCalls().values().forEach(PerformanceSampleAnalyzer::updateCounts);
        performanceSampleElement.setSamples(performanceSampleElement.getSamples() + performanceSampleElement.getCalls().values().stream().mapToLong(PerformanceSampleElement::getSamples).sum());
    }

    public static void merge(PerformanceSampleElement into, PerformanceSampleElement from) {
        into.setSamples(into.getSamples() + from.getSamples());
        Map<String, PerformanceSampleElement> intoCalls = into.getCalls();

        for (Map.Entry<String, PerformanceSampleElement> fromEntry : from.getCalls().entrySet()) {
            String key = fromEntry.getKey();
            PerformanceSampleElement value = fromEntry.getValue();

            PerformanceSampleElement existing = intoCalls.get(key);
            if (existing == null) {
                intoCalls.put(key, value);
            } else {
                merge(existing, value);
            }
        }
    }

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("i", FILE_OPT, true, "The file to analyze.");
        options.addOption("o", OUTPUT_FILE_OPT, true, "The output file (default json to stdout).");
        options.addOption("p", RELEVANT_PATTERN_OPT, true, "Pattern(s) to include as roots in the output (default: " + DEFAULT_PATTERN + ")");
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine commandLine = parser.parse(options, args);
            String file = commandLine.getOptionValue(FILE_OPT);
            if (StringUtils.isEmpty(file)) {
                printUsageAndExit("Must specify file", options, 1);
            }
            Pattern relevantPattern = Pattern.compile(commandLine.getOptionValue(RELEVANT_PATTERN_OPT, DEFAULT_PATTERN));
            PerformanceSampleElement performanceSampleElement = relevantElements(relevantPattern, new ObjectMapper().readValue(new File(file), PerformanceSampleElement.class));
            updateCounts(performanceSampleElement);
            String outputFile = commandLine.getOptionValue(OUTPUT_FILE_OPT);
            if (StringUtils.isEmpty(outputFile)) {
                new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(System.out, new OutputPerformanceSampleElement(performanceSampleElement));
            } else {
                new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new File(outputFile), new OutputPerformanceSampleElement(performanceSampleElement));
            }
        } catch (Exception e) {
            e.printStackTrace();
            printUsageAndExit(e.getMessage(), options, 2);
        }
    }
}
