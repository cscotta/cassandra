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
 * Compression-codec specification for one side of a comparison.
 *
 * <p>Maps to the CQL {@code WITH compression = { 'class': ..., 'chunk_length_in_kb': ..., ... }}
 * clause. Class is the codec name (e.g. {@code LZ4Compressor}, {@code ZstdCompressor},
 * {@code SnappyCompressor}, {@code DeflateCompressor}); options can include
 * {@code chunk_length_in_kb}, {@code level} for Zstd, etc.
 */
public final class CompressionSpec
{
    public String className;
    public Map<String, String> options = new LinkedHashMap<>();

    public CompressionSpec() {}

    public CompressionSpec(String className, Map<String, String> options)
    {
        this.className = className;
        this.options = options == null ? new LinkedHashMap<>() : new LinkedHashMap<>(options);
    }

    public void setClass(String className) { this.className = className; }
    public String getClassName() { return className; }
    public Map<String, String> getOptions() { return Collections.unmodifiableMap(options); }

    /** @return CQL fragment for {@code WITH compression = ...} */
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
        return "CompressionSpec{class=" + className + ", options=" + options + '}';
    }
}
