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

import org.apache.cassandra.db.AbstractCompactionController;
import org.apache.cassandra.utils.TimeUUID;

/**
 * Bridge class for the compaction validator tool that allows direct instantiation of either
 * {@link CursorCompactionPipeline} or {@link IteratorCompactionPipeline} without consulting the global
 * {@code DatabaseDescriptor.cursorCompactionEnabled()} flag.
 *
 * <p>This class lives in {@code org.apache.cassandra.db.compaction} so that it has package-private
 * access to the pipeline implementations, following the same pattern as
 * {@code tools/stress/src/org/apache/cassandra/io/sstable/StressCQLSSTableWriter.java}.
 */
public final class PipelineSelector
{
    /** The compaction backend to use. */
    public enum Backend
    {
        /** Use {@link IteratorCompactionPipeline} (the legacy row-iterator-based path). */
        ITERATOR,
        /** Use {@link CursorCompactionPipeline} (the newer cursor-based path). */
        CURSOR
    }

    private PipelineSelector()
    {
        // utility class
    }

    /**
     * Creates a compaction pipeline using the specified backend, bypassing the global
     * {@code DatabaseDescriptor.cursorCompactionEnabled()} flag.
     *
     * @param backend     which pipeline implementation to instantiate
     * @param task        the compaction task supplying writer and directory resolution
     * @param type        the operation type (e.g. {@link OperationType#COMPACTION})
     * @param scanners    the scanner list wrapping the input SSTables
     * @param controller  the compaction controller governing GC and tombstone decisions
     * @param nowInSec    the wall-clock time (seconds since epoch) used for TTL evaluation
     * @param compactionId the unique identifier for this compaction operation
     * @return an {@link AbstractCompactionPipeline} ready to be used in a compaction loop
     * @throws IllegalArgumentException if {@code backend} is {@link Backend#CURSOR} but
     *                                  {@link CursorCompactor#isSupported} returns {@code false}
     *                                  for the given scanners and controller
     */
    public static AbstractCompactionPipeline create(Backend backend,
                                                    CompactionTask task,
                                                    OperationType type,
                                                    AbstractCompactionStrategy.ScannerList scanners,
                                                    AbstractCompactionController controller,
                                                    long nowInSec,
                                                    TimeUUID compactionId)
    {
        switch (backend)
        {
            case CURSOR:
                if (!CursorCompactor.isSupported(scanners, controller))
                    throw new IllegalArgumentException(
                        "CursorCompactionPipeline is not supported for the given scanners and controller. " +
                        "Use Backend.ITERATOR or check CursorCompactor.isSupported() before calling create().");
                return new CursorCompactionPipeline(task, type, scanners, controller, nowInSec, compactionId);
            case ITERATOR:
                return new IteratorCompactionPipeline(task, type, scanners, controller, nowInSec, compactionId);
            default:
                throw new AssertionError("Unknown backend: " + backend);
        }
    }
}
