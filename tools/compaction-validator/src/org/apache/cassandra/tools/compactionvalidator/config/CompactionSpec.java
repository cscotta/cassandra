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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Compaction-strategy specification for one side of a comparison.
 *
 * <p>The {@code class} field is the strategy class name as used in CQL (e.g.
 * {@code UnifiedCompactionStrategy}, {@code SizeTieredCompactionStrategy}); the
 * {@code options} map holds the per-strategy tunables that go into the
 * {@code WITH compaction = { ... }} clause of the {@code CREATE TABLE} statement.
 *
 * <p>Public mutable fields plus a public no-arg constructor are required so
 * SnakeYAML can deserialise instances via reflection. After the parser loads
 * the file, instances are conceptually immutable for the rest of the run.
 */
public final class CompactionSpec
{
    public String className;
    public Map<String, String> options = new LinkedHashMap<>();

    public CompactionSpec() {}

    public CompactionSpec(String className, Map<String, String> options)
    {
        this.className = className;
        this.options = options == null ? new LinkedHashMap<>() : new LinkedHashMap<>(options);
    }

    /** SnakeYAML setter — our YAML uses `class:` (not the Java keyword) for readability. */
    public void setClass(String className) { this.className = className; }
    public String getClassName() { return className; }
    public Map<String, String> getOptions() { return Collections.unmodifiableMap(options); }

    /**
     * @return CQL fragment suitable for {@code WITH compaction = ...}, e.g.
     *         {@code {'class':'UnifiedCompactionStrategy','target_sstable_size':'64MiB'}}
     */
    public String toCqlOptions()
    {
        StringBuilder sb = new StringBuilder("{'class': '").append(className).append('\'');
        for (Map.Entry<String, String> e : options.entrySet())
            sb.append(", '").append(e.getKey()).append("': '").append(e.getValue()).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public String toString()
    {
        return "CompactionSpec{class=" + className + ", options=" + options + '}';
    }
}
