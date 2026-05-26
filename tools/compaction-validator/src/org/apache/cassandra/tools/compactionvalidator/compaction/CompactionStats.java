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
package org.apache.cassandra.tools.compactionvalidator.compaction;

/**
 * Mutable statistics for a single compaction run (legacy or cursor-based).
 *
 * <p>Fields are declared {@code volatile} so that a monitoring thread can read
 * live progress while a compaction thread updates them, without requiring
 * explicit synchronization on the hot path.
 *
 * <p>After a run completes, {@link #durationMs} must be set by the calling
 * code before throughput metrics are queried.
 */
public class CompactionStats
{
    /** Number of partitions emitted by the compaction so far. */
    public volatile long partitionsProcessed;

    /** Number of rows (cells) emitted by the compaction so far. */
    public volatile long rowsProcessed;

    /** Uncompressed bytes written so far. */
    public volatile long bytesProcessed;

    /** Total elapsed wall-clock time for the run, in milliseconds. */
    public volatile long durationMs;

    /** Number of input SSTables fed into this compaction. */
    public volatile int sstablesIn;

    /** Number of output SSTables produced by this compaction. */
    public volatile int sstablesOut;

    /**
     * Accumulates progress from a single batch.
     *
     * @param partitions number of partitions processed in this batch
     * @param rows       number of rows processed in this batch
     * @param bytes      uncompressed bytes processed in this batch
     */
    public void recordProgress(long partitions, long rows, long bytes)
    {
        partitionsProcessed += partitions;
        rowsProcessed += rows;
        bytesProcessed += bytes;
    }

    /**
     * Returns the average write throughput in MiB/s for the entire run.
     *
     * <p>Returns {@code 0.0} if {@link #durationMs} is zero (run has not
     * completed or completed instantaneously).
     */
    public double throughputMiBs()
    {
        if (durationMs == 0)
            return 0.0;
        double seconds = durationMs / 1000.0;
        return bytesProcessed / seconds / (1024.0 * 1024.0);
    }

    /**
     * Returns the average partition throughput in partitions/s for the entire
     * run.
     *
     * <p>Returns {@code 0.0} if {@link #durationMs} is zero.
     */
    public double partitionsPerSecond()
    {
        if (durationMs == 0)
            return 0.0;
        double seconds = durationMs / 1000.0;
        return partitionsProcessed / seconds;
    }
}
