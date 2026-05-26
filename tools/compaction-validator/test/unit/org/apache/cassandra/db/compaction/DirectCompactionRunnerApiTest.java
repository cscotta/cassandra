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
package org.apache.cassandra.db.compaction;

import org.junit.Test;

/**
 * Compile-time API surface guard for {@link DirectCompactionRunner}.
 *
 * <p>This test has no runtime assertions.  Its sole purpose is to fail at <em>compile time</em> if
 * the protected fields and methods that {@link DirectCompactionRunner} relies on are renamed,
 * removed, or have their visibility reduced in a future refactoring.  The build will then surface
 * the breakage immediately rather than at run-time inside the validator tool.
 *
 * <p>Fields referenced:
 * <ul>
 *   <li>{@code CompactionTask.gcBeforeSeconds} — {@code protected final long}</li>
 *   <li>{@code CompactionTask.keepOriginals} — {@code protected final boolean}</li>
 *   <li>{@code AbstractCompactionTask.cfs} — {@code protected final ColumnFamilyStore}</li>
 *   <li>{@code AbstractCompactionTask.compactionType} — {@code protected OperationType}</li>
 * </ul>
 */
public class DirectCompactionRunnerApiTest
{
    /**
     * Verifies that the protected API surface of {@link CompactionTask} and
     * {@link AbstractCompactionTask} that {@link DirectCompactionRunner} depends on still compiles.
     *
     * <p>If any of the field references below fail to compile, it means the production code was
     * changed in a way that breaks the tool and the tool source must be updated accordingly.
     */
    @Test
    @SuppressWarnings("unused")
    public void apiSurfaceStillExists()
    {
        // Anonymous subclass to gain access to protected members without constructing a real
        // CompactionTask (which requires a live ColumnFamilyStore and ILifecycleTransaction).
        // The class body is never instantiated at runtime; javac just needs to resolve the fields.
        abstract class Probe extends CompactionTask
        {
            Probe()
            {
                super(null, null, 0L);
            }

            void checkFields()
            {
                // CompactionTask protected fields
                long gcBefore = gcBeforeSeconds;
                boolean keep = keepOriginals;

                // AbstractCompactionTask protected fields
                org.apache.cassandra.db.ColumnFamilyStore store = cfs;
                OperationType opType = compactionType;
            }
        }
        // The class body above is the compile-time check; nothing to assert at runtime.
    }
}
