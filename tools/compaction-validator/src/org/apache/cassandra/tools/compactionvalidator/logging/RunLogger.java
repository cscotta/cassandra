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
package org.apache.cassandra.tools.compactionvalidator.logging;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.cassandra.tools.compactionvalidator.RunResult;
import org.apache.cassandra.tools.compactionvalidator.compaction.CompactionStats;
import org.apache.cassandra.tools.compactionvalidator.validation.ErrataRule;
import org.apache.cassandra.tools.compactionvalidator.validation.FormatViolation;
import org.apache.cassandra.tools.compactionvalidator.validation.ValidationStats;

/**
 * Append-only structured log writer for compaction-validator runs.
 *
 * <p>Opens the target file with {@code APPEND | CREATE} so multiple invocations
 * accumulate in the same file.  Call {@link #writeHeader()} once at startup,
 * {@link #appendRunEntry(RunResult)} after each run, and {@link #close()} when
 * done.
 */
public class RunLogger implements Closeable
{
    private static final DateTimeFormatter ISO_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private final BufferedWriter writer;

    /**
     * Opens {@code logFile} for appending, creating it if it does not exist.
     *
     * @param logFile target log file
     * @throws IOException if the file cannot be opened
     */
    public RunLogger(File logFile) throws IOException
    {
        this.writer = new BufferedWriter(
            new OutputStreamWriter(
                Files.newOutputStream(logFile.toPath(),
                                     StandardOpenOption.APPEND,
                                     StandardOpenOption.CREATE),
                StandardCharsets.UTF_8));
    }

    /**
     * Writes a startup header that includes the current timestamp plus system
     * information gathered from {@link SystemInfo}.
     *
     * @throws IOException if writing fails
     */
    public void writeHeader() throws IOException
    {
        String timestamp = ISO_FORMATTER.format(Instant.now());
        writer.write("================================================================================");
        writer.newLine();
        writer.write("[" + timestamp + "] compaction-validator started");
        writer.newLine();
        writer.write(SystemInfo.summary());
        writer.newLine();
        writer.write("================================================================================");
        writer.newLine();
        writer.flush();
    }

    /**
     * Appends a structured entry for one completed run.
     *
     * <p>On success the entry looks like:
     * <pre>
     * [2026-05-24T14:32:09Z] Run #47 seed=0xDEADBEEF12345678
     *   Schema:  cvtest_deadbeef.t  (5 PK, 2 CK, 3 static, 8 regular cols)
     *   UCS:     target_sstable_size=256MiB  base_shard_count=2
     *   DataGen: 10.3 GiB  |  1.4M partitions  |  52M rows  |  310 MiB/s avg
     *   Legacy:  18.4s  |  290 MiB/s  |  1.4M partitions/s
     *   Cursor:  15.7s  |  340 MiB/s  |  1.6M partitions/s  |  Speedup: 1.17x
     *   Valid:   PASS  |  1.4M partitions matched  |  52M rows matched
     * </pre>
     *
     * <p>On failure the {@code Valid} line is replaced with mismatch details and
     * re-run instructions are appended.
     *
     * @param result the completed run result
     * @throws IOException if writing fails
     */
    public void appendRunEntry(RunResult result) throws IOException
    {
        String timestamp = ISO_FORMATTER.format(Instant.ofEpochMilli(result.startTimeMs));

        writer.write(String.format("[%s] Run #%d seed=0x%016X",
                                   timestamp, result.runNumber, result.seed));
        writer.newLine();

        // Schema line
        writer.write(String.format("  Schema:  %s.%s  (%d PK, %d CK, %d static, %d regular cols, shape=%s, gc_grace=%ds)",
                                   result.keyspaceName, result.tableName,
                                   result.partitionKeyCount, result.clusteringKeyCount,
                                   result.staticColumnCount, result.regularColumnCount,
                                   result.schemaShape,
                                   result.gcGraceSeconds));
        writer.newLine();

        // UCS configuration line
        writer.write(String.format("  UCS:     target_sstable_size=%dMiB  base_shard_count=%d  compression_chunk=%dKiB",
                                   result.targetSstableSizeMiB, result.baseShardCount,
                                   result.compressionChunkKb));
        writer.newLine();

        // Full schema CQL — useful for both normal-run audits and post-mortem analysis. We
        // indent each line so it stays distinguishable from the key:value lines above and
        // can be scanned/parsed (or passed back to cqlsh) by tooling.
        if (result.schemaCql != null && !result.schemaCql.isEmpty())
        {
            writer.write("  Schema CQL:");
            writer.newLine();
            for (String line : result.schemaCql.split("\\R"))
            {
                writer.write("    " + line);
                writer.newLine();
            }
        }

        // Data generation stats
        if (result.dataGenStats != null)
        {
            long genBytes = result.dataGenStats.getBytesWritten();
            long genPartitions = result.dataGenStats.getPartitionsWritten();
            long genRows = result.dataGenStats.getRowsWritten();
            double genMiBs = result.dataGenStats.throughputMiBs();

            writer.write(String.format("  DataGen: %s  |  %s partitions  |  %s rows  |  %.0f MiB/s avg",
                                       formatBytes(genBytes),
                                       formatCount(genPartitions),
                                       formatCount(genRows),
                                       genMiBs));
            writer.newLine();
        }

        // Per-side display labels (8-char field for alignment with the rest of the
        // log entry's left margin). Falls back to "Control" / "Experiment" if the
        // result didn't carry side names — kept defensive so older code paths that
        // don't set them on the builder still produce readable output.
        String controlLabel    = padLabel(result.controlName != null ? result.controlName : "control");
        String experimentLabel = padLabel(result.experimentName != null ? result.experimentName : "experiment");

        // Control-side compaction stats
        if (result.legacyStats != null)
        {
            CompactionStats ls = result.legacyStats;
            double legacySecs = ls.durationMs / 1000.0;
            writer.write(String.format("  %s %.1fs  |  %.0f MiB/s  |  %s partitions/s",
                                       controlLabel,
                                       legacySecs,
                                       ls.throughputMiBs(),
                                       formatCount((long) ls.partitionsPerSecond())));
            writer.newLine();
        }

        // Experiment-side compaction stats
        if (result.cursorStats != null)
        {
            CompactionStats cs = result.cursorStats;
            double cursorSecs = cs.durationMs / 1000.0;
            double speedup = result.cursorSpeedup();
            writer.write(String.format("  %s %.1fs  |  %.0f MiB/s  |  %s partitions/s  |  Speedup: %.2fx",
                                       experimentLabel,
                                       cursorSecs,
                                       cs.throughputMiBs(),
                                       formatCount((long) cs.partitionsPerSecond()),
                                       speedup));
            writer.newLine();
        }

        // Validation result
        ValidationStats vs = result.validationStats;
        if (result.isSuccess())
        {
            String partStr = vs != null ? formatCount(vs.partitionsChecked.get()) : "?";
            String rowStr  = vs != null ? formatCount(vs.rowsChecked.get()) : "?";
            writer.write(String.format("  Valid:   PASS  |  %s partitions matched  |  %s rows matched",
                                       partStr, rowStr));
            writer.newLine();

            // Errata block — only emitted when at least one rule fired this run.
            // Each line names the rule + count so a grep across the long-running log
            // gives a quick "are we still seeing this known bug?" signal.
            if (vs != null && vs.totalErrataOccurrences() > 0)
            {
                writer.write(String.format("  Errata:  %d suppressed (run still PASS):",
                                           vs.totalErrataOccurrences()));
                writer.newLine();
                for (Map.Entry<ErrataRule, AtomicLong> e
                     : vs.errataOccurrences.entrySet())
                {
                    long n = e.getValue().get();
                    if (n == 0) continue;
                    writer.write(String.format("    %-30s  %d", e.getKey().cliName(), n));
                    writer.newLine();
                }
            }
        }
        else
        {
            writer.write("  Valid:   FAIL  |  " + (result.failureDetail != null ? result.failureDetail : "unknown error"));
            writer.newLine();

            if (result.preservedDir != null)
            {
                writer.write("  Preserved: " + result.preservedDir);
                writer.newLine();
            }

            writer.write(String.format("  Re-run:  compaction-validator --seed 0x%016X --once --no-cleanup",
                                       result.seed));
            writer.newLine();

            // Full mismatch dump — partition key, partition-level deletion, static row, and the
            // first ~64 unfiltereds from each side with metadata-aware toString. The bulk of the
            // diagnostic information lives in here; preserved alongside the SSTables on disk it
            // gives a developer everything they need to begin investigating without re-running.
            if (result.mismatchReport != null)
            {
                writer.write("  Mismatch detail:");
                writer.newLine();
                for (String line : result.mismatchReport.formatForDisplay().split("\\R"))
                {
                    writer.write("    " + line);
                    writer.newLine();
                }
            }
        }

        // Format-violation block — emitted whenever the FormatAuditor reported something,
        // regardless of pass/fail. Top-line shows total + per-kind counts; full per-violation
        // detail follows so a grep on the log gives both quick ("how many?") and deep
        // ("which cells?") investigations from the same artifact.
        if (result.formatViolations != null && !result.formatViolations.isEmpty())
        {
            Map<FormatViolation.Kind, Integer> kindCounts =
                new EnumMap<>(FormatViolation.Kind.class);
            for (FormatViolation v : result.formatViolations)
                kindCounts.merge(v.kind, 1, Integer::sum);

            writer.write(String.format("  Format:  %d violation(s) on experiment side:",
                                       result.formatViolations.size()));
            writer.newLine();
            for (Map.Entry<FormatViolation.Kind, Integer> e
                 : kindCounts.entrySet())
            {
                writer.write(String.format("    %-32s  %d", e.getKey().cliName(), e.getValue()));
                writer.newLine();
            }
            // Cap the per-violation detail dump at 32 entries so a runaway audit doesn't
            // blow the log file up. Anything past that is summarised in the line above.
            int capped = Math.min(32, result.formatViolations.size());
            for (int i = 0; i < capped; i++)
            {
                FormatViolation v = result.formatViolations.get(i);
                writer.write("    [" + v.kind.cliName() + "] sstable=" + shortName(v.sstableFilename)
                             + " pk=" + v.partitionKeyHex + " — " + v.detail);
                writer.newLine();
            }
            if (result.formatViolations.size() > capped)
            {
                writer.write(String.format("    ... %d more violations elided",
                                           result.formatViolations.size() - capped));
                writer.newLine();
            }
        }

        writer.flush();
    }

    /**
     * Closes the underlying file writer, flushing any buffered output first.
     *
     * @throws IOException if flushing or closing fails
     */
    @Override
    public void close() throws IOException
    {
        writer.close();
    }

    // -------------------------------------------------------------------------
    // Formatting helpers
    // -------------------------------------------------------------------------

    /**
     * Formats a byte count as a human-readable string with up to one decimal
     * place and an appropriate unit (GiB / MiB / KiB / B).
     */
    /**
     * Right-pads a side label to a fixed 9-character field followed by a colon, so
     * different runs with side names of varying length still line up to a consistent
     * left margin (e.g. {@code "legacy:   "} vs {@code "stcs:     "}). Truncates
     * names longer than 8 characters with a trailing dot, since the alternative is
     * misaligning every subsequent line.
     */
    private static String padLabel(String name)
    {
        String n = name == null ? "" : name;
        if (n.length() > 8)
            n = n.substring(0, 7) + ".";
        StringBuilder sb = new StringBuilder(10);
        sb.append(n).append(':');
        while (sb.length() < 10)
            sb.append(' ');
        return sb.toString();
    }

    private static String formatBytes(long bytes)
    {
        if (bytes >= 1024L * 1024L * 1024L)
            return String.format("%.1f GiB", bytes / (1024.0 * 1024.0 * 1024.0));
        if (bytes >= 1024L * 1024L)
            return String.format("%.1f MiB", bytes / (1024.0 * 1024.0));
        if (bytes >= 1024L)
            return String.format("%.1f KiB", bytes / 1024.0);
        return bytes + " B";
    }

    /**
     * Formats a large integer count with a "M" or "K" suffix for readability.
     */
    private static String formatCount(long count)
    {
        if (count >= 1_000_000L)
            return String.format("%.1fM", count / 1_000_000.0);
        if (count >= 1_000L)
            return String.format("%.1fK", count / 1_000.0);
        return Long.toString(count);
    }

    /**
     * Returns the basename of an absolute SSTable path so format-violation lines stay
     * single-screen-readable. Falls back to the full path when the input has no
     * separator.
     */
    private static String shortName(String fullPath)
    {
        if (fullPath == null) return "(unknown)";
        int slash = Math.max(fullPath.lastIndexOf('/'), fullPath.lastIndexOf('\\'));
        return slash >= 0 ? fullPath.substring(slash + 1) : fullPath;
    }
}
