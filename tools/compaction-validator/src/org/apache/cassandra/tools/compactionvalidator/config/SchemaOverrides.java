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
 * Schema-generation overrides. Anything left {@code null} stays per-seed
 * randomised by {@link org.apache.cassandra.tools.compactionvalidator.schema.SchemaGenerator}.
 *
 * <p>The fields here are properties shared by BOTH sides of a comparison —
 * partitioner, column-count constraints, etc. Per-side variations
 * (compaction strategy, compression) live on {@link SideConfig}.
 */
public final class SchemaOverrides
{
    /**
     * Partitioner: {@code Murmur3} (default in Cassandra) or
     * {@code RandomPartitioner}. {@code null} → use Murmur3 (the only
     * partitioner the cursor pipeline currently supports — cf
     * {@code CursorCompactor.isSupported}).
     */
    public String partitioner;

    public SchemaOverrides() {}

    public String getPartitioner() { return partitioner; }

    @Override
    public String toString()
    {
        return "SchemaOverrides{partitioner=" + partitioner + '}';
    }
}
