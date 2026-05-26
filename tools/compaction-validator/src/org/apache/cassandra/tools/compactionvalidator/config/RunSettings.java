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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Run-loop / environment knobs that used to be CLI flags. Replaces most of the
 * old picocli-driven {@code Main} fields. Mutable to allow SnakeYAML population
 * + a light merge step that lets {@code --seed}, {@code --working-dir},
 * {@code --log-file}, {@code --once} on the CLI override their YAML equivalents.
 */
public final class RunSettings
{
    /**
     * Approximate per-run target for generated SSTable bytes. Strings like
     * {@code 10G} / {@code 500MiB} are accepted and parsed by
     * {@link org.apache.cassandra.tools.compactionvalidator.util.ByteUtil#parseBytes}.
     */
    public String targetBytes = "10G";
    public int datagenThreads = Runtime.getRuntime().availableProcessors();
    public int compactionThreads = 1;
    public int validationThreads = 1;
    public int maxRuns = 0;
    public boolean noCleanup = false;
    public boolean noUi = false;

    /**
     * Errata rule names to suppress. Each entry must match one of
     * {@link org.apache.cassandra.tools.compactionvalidator.validation.ErrataRule#cliName()}
     * — unknown names fail validation at parse time so typos surface early.
     */
    public List<String> ignoreErrata = new ArrayList<>();

    public RunSettings() {}

    public String getTargetBytes() { return targetBytes; }
    public int getDatagenThreads() { return datagenThreads; }
    public int getCompactionThreads() { return compactionThreads; }
    public int getValidationThreads() { return validationThreads; }
    public int getMaxRuns() { return maxRuns; }
    public boolean getNoCleanup() { return noCleanup; }
    public boolean getNoUi() { return noUi; }
    public List<String> getIgnoreErrata() { return Collections.unmodifiableList(ignoreErrata); }

    // SnakeYAML accepts both camelCase and snake_case via these aliases.
    public void setTarget_bytes(String s) { this.targetBytes = s; }
    public void setDatagen_threads(int n) { this.datagenThreads = n; }
    public void setCompaction_threads(int n) { this.compactionThreads = n; }
    public void setValidation_threads(int n) { this.validationThreads = n; }
    public void setMax_runs(int n) { this.maxRuns = n; }
    public void setNo_cleanup(boolean b) { this.noCleanup = b; }
    public void setNo_ui(boolean b) { this.noUi = b; }
    public void setIgnore_errata(List<String> rules) { this.ignoreErrata = rules == null ? new ArrayList<>() : new ArrayList<>(rules); }

    @Override
    public String toString()
    {
        return "RunSettings{targetBytes=" + targetBytes
               + ", datagenThreads=" + datagenThreads
               + ", compactionThreads=" + compactionThreads
               + ", validationThreads=" + validationThreads
               + ", maxRuns=" + maxRuns
               + ", noCleanup=" + noCleanup
               + ", noUi=" + noUi
               + ", ignoreErrata=" + ignoreErrata + '}';
    }
}
