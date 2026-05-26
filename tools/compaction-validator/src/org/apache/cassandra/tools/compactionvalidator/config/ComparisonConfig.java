/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cassandra.tools.compactionvalidator.config;

/**
 * Holder for the two sides of a comparison run. Both sides are required —
 * {@link #validate} fails fast if either is missing.
 */
public final class ComparisonConfig
{
    public SideConfig control;
    public SideConfig experiment;

    public ComparisonConfig() {}

    public SideConfig getControl() { return control; }
    public SideConfig getExperiment() { return experiment; }

    /** Names of both sides, lowercased; used as keyspace / log / TUI labels. */
    public String controlName() { return control != null && control.name != null ? control.name : "control"; }
    public String experimentName() { return experiment != null && experiment.name != null ? experiment.name : "experiment"; }

    @Override
    public String toString()
    {
        return "ComparisonConfig{control=" + control + ", experiment=" + experiment + '}';
    }
}
