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
package org.apache.cassandra.tools.compactionvalidator.validation;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.cassandra.db.ClusteringComparator;
import org.apache.cassandra.db.ClusteringPrefix;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.ColumnData;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.Unfiltered;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.io.sstable.ISSTableScanner;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.utils.ByteBufferUtil;

/**
 * Walks one or more SSTables after compaction and asserts a list of byte-/semantic-level
 * well-formedness invariants on every cell and row. Returns a structured list of
 * {@link FormatViolation}s; an empty list means the auditor saw nothing wrong.
 *
 * <p>The auditor is intentionally narrow: it only checks invariants that are easy to
 * derive from the public iterator API (so we don't have to re-implement the BIG
 * format byte parser) but that no path in standard Cassandra code currently asserts.
 * The two main targets are:
 *
 * <ol>
 *   <li><b>Cell flags mutual exclusivity</b> (compaction bug 1A). When a writer sets
 *       both {@code IS_DELETED_MASK} and {@code IS_EXPIRING_MASK} in a cell's flags
 *       byte, the deserialiser's {@code else if} chain takes the IS_EXPIRING branch
 *       and reads {@code (ttl, localDeletionTime)} where {@code localDeletionTime} is
 *       supposed to be the expiration time {@code writetime + ttl}. But the value on
 *       disk was the IS_DELETED-style deletion time, NOT the expiration formula —
 *       so checking {@code cell.localDeletionTime() == (cell.timestamp() / 1_000_000)
 *       + cell.ttl()} reliably catches the bug.</li>
 *   <li><b>Row clustering ordering</b>. Rows within a partition must be in strictly-
 *       increasing clustering order. A writer that emits them out of order causes
 *       indexed point reads to silently land in the wrong slice; reverse iteration
 *       can throw or return the wrong sequence.</li>
 * </ol>
 *
 * <p>Additional cheap derived checks (timestamp / TTL non-negativity) catch the
 * obvious "fabricated cell" failure modes without requiring byte-level parsing.
 *
 * <p>The auditor opens scanners on each input SSTable and walks them sequentially —
 * O(bytes) over the input. For typical 1 GiB validator runs this completes in a few
 * seconds and adds negligible overhead to the run.
 */
public final class FormatAuditor
{
    /**
     * Cassandra writes expiration times as {@code writetime + ttl} where {@code writetime
     * = timestamp / 1_000_000} (seconds). The formula is exact and deterministic — there's
     * no clock involved at audit time, so a tolerance of even 0 would be fine. We allow
     * ±2 seconds of slack purely to absorb any future change to how Cassandra rounds the
     * computation; if a future version starts rounding microseconds in either direction,
     * we'd rather not flag every cell as a false-positive bug 1A while we discover the
     * change.
     */
    private static final long EXPIRATION_TOLERANCE_SECONDS = 2L;

    private FormatAuditor()
    {
    }

    /**
     * Audits every SSTable in {@code sstables}, returning the cumulative list of
     * violations (or an empty list if all SSTables are clean).
     *
     * <p>The auditor never throws on a malformed cell — it records the violation and
     * keeps going so a single audit run reports every problem rather than just the
     * first. If iteration itself throws (e.g. corrupt index files), the throwable
     * is wrapped in a {@link FormatViolation.Kind#SCANNER_FAILURE} violation and
     * iteration moves on to the next SSTable.
     *
     * <p>Parallelised across the available CPU pool because (a) each per-SSTable
     * audit is independent (no shared mutable state past appending to {@code
     * violations}, which is wrapped in a synchronised list), and (b) on the
     * bug-1A cascade path each scan can spend seconds chewing through a corrupt
     * SSTable before throwing — running them sequentially serialises the wait
     * and leaves the rest of the CPU idle. Capped at the number of SSTables on
     * the experiment side, since adding more threads than SSTables is wasted
     * pool overhead.
     */
    public static List<FormatViolation> audit(Collection<SSTableReader> sstables, ClusteringComparator clusteringComparator)
    {
        if (sstables == null || sstables.isEmpty())
            return Collections.emptyList();

        // Synchronised so concurrent worker threads can append safely.
        // Order across SSTables doesn't matter for downstream processing —
        // the RunLogger groups violations by kind and dumps them all.
        List<FormatViolation> violations = java.util.Collections.synchronizedList(new ArrayList<>());

        int threads = Math.min(sstables.size(), Runtime.getRuntime().availableProcessors());
        if (threads <= 1)
        {
            for (SSTableReader reader : sstables)
                auditOneCatching(reader, clusteringComparator, violations);
            return new ArrayList<>(violations);
        }

        java.util.concurrent.ExecutorService pool =
            java.util.concurrent.Executors.newFixedThreadPool(threads, new java.util.concurrent.ThreadFactory()
            {
                private final java.util.concurrent.atomic.AtomicInteger n = new java.util.concurrent.atomic.AtomicInteger();
                @Override public Thread newThread(Runnable r)
                {
                    Thread t = new Thread(r, "compaction-validator-audit-" + n.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                }
            });
        List<java.util.concurrent.Future<?>> futures = new ArrayList<>(sstables.size());
        try
        {
            for (SSTableReader reader : sstables)
            {
                final SSTableReader r = reader;
                futures.add(pool.submit(() -> auditOneCatching(r, clusteringComparator, violations)));
            }
            for (java.util.concurrent.Future<?> f : futures)
            {
                try { f.get(); }
                catch (java.util.concurrent.ExecutionException ee) { /* per-SSTable failures already captured */ }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }
        finally
        {
            pool.shutdownNow();
        }
        return new ArrayList<>(violations);
    }

    /**
     * Run {@link #auditOne} on a single reader and capture any throwable as a
     * {@link FormatViolation.Kind#SCANNER_FAILURE} violation. Factored out of the
     * loop body so both the sequential and parallel audit paths share the same
     * exception-handling behaviour.
     */
    private static void auditOneCatching(SSTableReader reader,
                                         ClusteringComparator comparator,
                                         List<FormatViolation> out)
    {
        try
        {
            auditOne(reader, comparator, out);
        }
        catch (Throwable t)
        {
            // Record the failure under SCANNER_FAILURE (not ROW_OUT_OF_ORDER)
            // and move on — one corrupt SSTable shouldn't hide invariants in
            // the rest of the output set. Bug 1A's downstream symptom is a
            // CorruptSSTableException as the deserialiser drifts past the
            // misaligned flag-byte cell; mapping it under its own kind keeps
            // the suppression matcher accurate and the diagnostic line
            // honest about what actually happened.
            out.add(new FormatViolation(
                FormatViolation.Kind.SCANNER_FAILURE,
                reader.getFilename(),
                "(scan failed)",
                "scanner threw " + t.getClass().getSimpleName()
                + (t.getMessage() != null ? ": " + t.getMessage() : "")));
        }
    }

    private static void auditOne(SSTableReader reader,
                                 ClusteringComparator comparator,
                                 List<FormatViolation> out)
    {
        String sstable = reader.getFilename();
        try (ISSTableScanner scanner = reader.getScanner())
        {
            while (scanner.hasNext())
            {
                try (UnfilteredRowIterator partition = scanner.next())
                {
                    auditPartition(partition, comparator, sstable, out);
                }
            }
        }
    }

    /**
     * Audits a single in-memory partition iterator. Exposed as the unit-test entry
     * point so tests don't need to construct an on-disk SSTable just to verify
     * the cell-flag and ordering invariants. Production callers go through
     * {@link #audit(Collection, ClusteringComparator)} which opens scanners on
     * each SSTable and dispatches here.
     *
     * @param partition  the partition's unfiltered stream — caller closes
     * @param comparator clustering comparator (may be {@code null} to skip the
     *                   row-ordering check; useful for tests on tables with no CK)
     * @param sstable    label included in any reported violations (typically the
     *                   SSTable's filename); free-form for tests
     * @param out        the running violation list — appended to in place
     */
    public static void auditPartition(UnfilteredRowIterator partition,
                                       ClusteringComparator comparator,
                                       String sstable,
                                       List<FormatViolation> out)
    {
        String pkHex = hex(partition.partitionKey());

        // Static row sits outside the clustering ordering — audit its cells but
        // don't compare clusterings against the rest of the partition body.
        Row staticRow = partition.staticRow();
        if (staticRow != null && !staticRow.isEmpty())
            auditRow(staticRow, sstable, pkHex, out);

        ClusteringPrefix<?> previousClustering = null;
        while (partition.hasNext())
        {
            Unfiltered u = partition.next();
            if (u.isRow())
            {
                Row row = (Row) u;
                ClusteringPrefix<?> currentClustering = row.clustering();
                if (previousClustering != null && comparator != null)
                {
                    int cmp = comparator.compare(previousClustering, currentClustering);
                    if (cmp > 0)
                    {
                        // Strict reverse — unambiguous out-of-order writer bug.
                        out.add(new FormatViolation(
                            FormatViolation.Kind.ROW_OUT_OF_ORDER,
                            sstable,
                            pkHex,
                            "previous clustering " + clusteringHint(previousClustering)
                            + " > current clustering " + clusteringHint(currentClustering)));
                    }
                    else if (cmp == 0)
                    {
                        // Equal clusterings within a partition body — structurally
                        // invalid, but in practice almost always a downstream symptom
                        // of bug-1A: the misaligned deserialiser reads garbage that
                        // parses as a "row marker" with the same clustering as the
                        // previous row. Reported under its own kind so the errata
                        // matcher can suppress the cascade without also masking real
                        // strict-reverse bugs (which stay under ROW_OUT_OF_ORDER).
                        out.add(new FormatViolation(
                            FormatViolation.Kind.DUPLICATE_CLUSTERING,
                            sstable,
                            pkHex,
                            "duplicate clustering " + clusteringHint(currentClustering)));
                    }
                }
                previousClustering = currentClustering;
                auditRow(row, sstable, pkHex, out);
            }
            // RangeTombstoneMarkers and other Unfiltered kinds are skipped here:
            // their on-disk invariants overlap with the format paths we don't
            // catch via cell flags, so they're left to future work.
        }
    }

    private static void auditRow(Row row, String sstable, String pkHex, List<FormatViolation> out)
    {
        for (ColumnData cd : row)
        {
            if (cd instanceof Cell<?>)
                auditCell((Cell<?>) cd, sstable, pkHex, out);
            else if (cd instanceof org.apache.cassandra.db.rows.ComplexColumnData)
            {
                org.apache.cassandra.db.rows.ComplexColumnData complex =
                    (org.apache.cassandra.db.rows.ComplexColumnData) cd;
                for (Cell<?> sub : complex)
                    auditCell(sub, sstable, pkHex, out);
            }
        }
    }

    /**
     * Per-cell invariant checks. The cell flag invariants live here — the
     * {@code IS_DELETED} / {@code IS_EXPIRING} mutual-exclusivity rule is enforced
     * via the expiration formula (see class doc).
     */
    private static void auditCell(Cell<?> cell, String sstable, String pkHex, List<FormatViolation> out)
    {
        // Sanity: timestamps must be non-negative. CQL writes always produce positive
        // microsecond timestamps; a negative one in compacted output indicates a
        // fabricated cell.
        if (cell.timestamp() < 0)
        {
            out.add(new FormatViolation(
                FormatViolation.Kind.TIMESTAMP_NEGATIVE,
                sstable, pkHex,
                "column=" + cell.column().name + " timestamp=" + cell.timestamp()));
        }

        // TTL non-negativity: CQL only emits TTLs in [0, MaxTtl]; negative is invalid.
        if (cell.ttl() < 0)
        {
            out.add(new FormatViolation(
                FormatViolation.Kind.TTL_NEGATIVE,
                sstable, pkHex,
                "column=" + cell.column().name + " ttl=" + cell.ttl()));
        }

        // Bug 1A signature: an expiring cell whose localDeletionTime field doesn't
        // satisfy the expiration formula. The cleanest way to detect "writer set
        // both IS_DELETED and IS_EXPIRING" — reading the resulting cell looks like
        // a normal expiring cell except its expiration_time is wrong (a deletion
        // time was stored where the expiration formula's result should be).
        if (cell.isExpiring())
        {
            long writetimeSeconds = cell.timestamp() / 1_000_000L;
            long expectedLdt      = writetimeSeconds + cell.ttl();
            long actualLdt        = cell.localDeletionTime();
            long delta            = Math.abs(actualLdt - expectedLdt);
            if (delta > EXPIRATION_TOLERANCE_SECONDS)
            {
                out.add(new FormatViolation(
                    FormatViolation.Kind.FLAG_DELETE_AND_EXPIRE,
                    sstable, pkHex,
                    "column=" + cell.column().name
                    + " ttl=" + cell.ttl()
                    + " timestamp=" + cell.timestamp()
                    + " localDeletionTime=" + actualLdt
                    + " expectedLdt=" + expectedLdt
                    + " (writetime+ttl)"));
            }
        }
    }

    private static String hex(DecoratedKey key)
    {
        return ByteBufferUtil.bytesToHex(key.getKey());
    }

    /**
     * Renders a clustering prefix as a short hex string for use in violation
     * descriptions. Stable across runs (no toString variability) so test
     * assertions can match on it directly.
     */
    private static String clusteringHint(ClusteringPrefix<?> c)
    {
        if (c == null)
            return "(null)";
        StringBuilder sb = new StringBuilder().append('(');
        int n = c.size();
        for (int i = 0; i < n; i++)
        {
            if (i > 0) sb.append(',');
            ByteBuffer raw = c.bufferAt(i);
            if (raw == null)
                sb.append("null");
            else
                sb.append(ByteBufferUtil.bytesToHex(raw));
        }
        sb.append(')');
        return sb.toString();
    }
}
