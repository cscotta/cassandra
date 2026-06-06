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

import java.util.Objects;

/**
 * Top-level configuration for one validator invocation, loaded from YAML by
 * {@link ConfigParser}. Everything the validator needs to know about a run
 * (other than environment-specific paths and the seed) lives in here.
 */
public final class RunConfig
{
    /** Schema-version field for forward-compatible parsing. Currently 1. */
    public int version = 1;

    public RunSettings run = new RunSettings();
    public SchemaOverrides schema = new SchemaOverrides();
    public ComparisonConfig comparison;

    public RunConfig() {}

    public int getVersion() { return version; }
    public RunSettings getRun() { return run; }
    public SchemaOverrides getSchema() { return schema; }
    public ComparisonConfig getComparison() { return comparison; }

    /**
     * Sanity-checks a freshly-parsed config and throws
     * {@link IllegalArgumentException} on the first problem. Caught by
     * {@link org.apache.cassandra.tools.compactionvalidator.Main} and
     * re-thrown as a user-facing message before any work begins.
     */
    public void validate()
    {
        if (version != 1)
            throw new IllegalArgumentException("unsupported config version " + version + " (expected 1)");
        if (comparison == null || comparison.control == null || comparison.experiment == null)
            throw new IllegalArgumentException("config must declare comparison.control and comparison.experiment");
        if (run == null)
            throw new IllegalArgumentException("config must declare a `run:` block (use empty {} for all defaults)");
        if (run.datagenThreads <= 0)
            throw new IllegalArgumentException("run.datagen_threads must be > 0");
        if (run.compactionThreads <= 0)
            throw new IllegalArgumentException("run.compaction_threads must be > 0");
        if (run.validationThreads <= 0)
            throw new IllegalArgumentException("run.validation_threads must be > 0");
        if (run.maxRuns < 0)
            throw new IllegalArgumentException("run.max_runs must be >= 0 (0 means unlimited)");
        // First-cut limitation: io_mode must match across sides. See SideConfig.ioMode docs.
        String controlIo = comparison.control.ioMode;
        String experimentIo = comparison.experiment.ioMode;
        if (!Objects.equals(controlIo, experimentIo))
            throw new IllegalArgumentException(
                "comparison.control.io_mode (" + controlIo + ") must equal "
                + "comparison.experiment.io_mode (" + experimentIo + "). Per-side I/O modes "
                + "are not supported in the same JVM yet — DatabaseDescriptor.disk_access_mode "
                + "is process-global. Run two separate invocations to compare across modes.");
    }

    @Override
    public String toString()
    {
        return "RunConfig{version=" + version
               + ", run=" + run
               + ", schema=" + schema
               + ", comparison=" + comparison + '}';
    }
}
