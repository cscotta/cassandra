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
package org.apache.cassandra.tools.compactionvalidator.datagen;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Accumulates statistics about SSTable data generation.
 *
 * <p>All fields are {@link AtomicLong} so that multiple writer threads can update them
 * concurrently without external synchronisation.
 */
public final class DataGenStats
{
    /** Total bytes written to disk (sum of on-disk SSTable sizes). */
    public final AtomicLong bytesWritten = new AtomicLong(0L);

    /**
     * Snapshot of {@link #rowsWritten} taken each time {@link #recordBytes(long)} fires.
     * Used together with {@link #bytesWritten} to derive bytes-per-row, which lets
     * {@link #estimatedBytesWritten()} extrapolate in-flight bytes between SSTable
     * flushes (the writer buffers aggressively, so {@code bytesWritten} can sit at
     * zero for a long time while millions of rows are buffered in memory).
     */
    public final AtomicLong rowsAtLastFlush = new AtomicLong(0L);

    /**
     * Running max of {@link #estimatedBytesWritten()}. The estimate uses two regimes
     * (rows × default bytes/row before any flush has calibrated, then flushed-bytes
     * + extrapolation after) and the transition can snap backwards when the
     * default 64 b/row estimate over-shoots the schema's actual size. Clamping the
     * exposed value to its running max means progress bars never reverse — which
     * matters more for UX than the brief over-estimate before calibration.
     */
    private final AtomicLong estimatedHighWatermark = new AtomicLong(0L);

    /** Total elapsed wall-clock time for the generation phase, in milliseconds. */
    public volatile long durationMs;

    /** Total number of partitions written across all threads. */
    public final AtomicLong partitionsWritten = new AtomicLong(0L);

    /** Total number of CQL rows (clustering rows) written across all threads. */
    public final AtomicLong rowsWritten = new AtomicLong(0L);

    /** Adds {@code n} to the bytes-written counter. */
    public void recordBytes(long n)
    {
        bytesWritten.addAndGet(n);
        // Stamp the row count at this flush so estimatedBytesWritten() can extrapolate.
        rowsAtLastFlush.set(rowsWritten.get());
    }

    /** Increments the partition counter by one. */
    public void recordPartition()
    {
        partitionsWritten.incrementAndGet();
    }

    /** Adds {@code n} to the rows-written counter. */
    public void recordRows(long n)
    {
        rowsWritten.addAndGet(n);
    }

    /** @return total bytes written to disk */
    public long getBytesWritten()
    {
        return bytesWritten.get();
    }

    /** @return total partitions written */
    public long getPartitionsWritten()
    {
        return partitionsWritten.get();
    }

    /** @return total CQL rows written */
    public long getRowsWritten()
    {
        return rowsWritten.get();
    }

    /**
     * Returns an extrapolated total of bytes that have been "produced" so far —
     * actual flushed bytes plus an estimate for rows currently buffered in the writer
     * but not yet flushed to disk. Suitable for driving a progress bar; not for
     * authoritative reporting.
     *
     * <p>Uses three regimes, in order of preference:
     * <ol>
     *   <li>If at least one SSTable has flushed: bytes-per-row is computed from the
     *       flushed total, and applied to the rows accumulated since that flush.</li>
     *   <li>If nothing has flushed yet but rows have been recorded: a rough default
     *       of {@value #DEFAULT_BYTES_PER_ROW_ESTIMATE} bytes per row is applied so
     *       the progress bar moves visibly while the writer is still buffering.</li>
     *   <li>Otherwise zero.</li>
     * </ol>
     */
    public long estimatedBytesWritten()
    {
        long flushed = bytesWritten.get();
        long rowsAtFlush = rowsAtLastFlush.get();
        long currentRows = rowsWritten.get();
        long raw;
        if (flushed > 0 && rowsAtFlush > 0)
        {
            long inFlightRows = Math.max(0L, currentRows - rowsAtFlush);
            if (inFlightRows == 0L)
                raw = flushed;
            else
            {
                long bytesPerRow = flushed / rowsAtFlush;
                raw = flushed + (inFlightRows * bytesPerRow);
            }
        }
        else
        {
            // No calibration yet — estimate from rows alone so the progress bar still moves
            // during the initial buffering period (which can span the entire generation when
            // the writer's max-sstable-size exceeds the run's target bytes).
            raw = currentRows * DEFAULT_BYTES_PER_ROW_ESTIMATE;
        }
        // Clamp to running max so the displayed progress is monotonic. Once the first flush
        // calibrates bytes/row to a value below the 64-byte default, the raw estimate drops;
        // we'd rather show a brief plateau than a backwards jump.
        long prev;
        do
        {
            prev = estimatedHighWatermark.get();
            if (raw <= prev)
                return prev;
        }
        while (!estimatedHighWatermark.compareAndSet(prev, raw));
        return raw;
    }

    /**
     * Pre-calibration row-size estimate (bytes). Picked to slightly under-estimate so
     * the progress bar doesn't run ahead and then have to snap backwards when real
     * flush data arrives. Production runs see ~50–200 bytes/row depending on schema.
     */
    private static final long DEFAULT_BYTES_PER_ROW_ESTIMATE = 64L;

    /**
     * Returns the average write throughput in MiB/s for the data generation
     * phase.  Returns {@code 0.0} if {@link #durationMs} is zero.
     */
    public double throughputMiBs()
    {
        if (durationMs == 0)
            return 0.0;
        double seconds = durationMs / 1000.0;
        return bytesWritten.get() / seconds / (1024.0 * 1024.0);
    }
}
