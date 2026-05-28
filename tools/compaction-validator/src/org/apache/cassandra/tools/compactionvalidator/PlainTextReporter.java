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
package org.apache.cassandra.tools.compactionvalidator;

import java.io.PrintStream;

import org.apache.cassandra.tools.compactionvalidator.compaction.CompactionStats;
import org.apache.cassandra.tools.compactionvalidator.data.DataGenStats;
import org.apache.cassandra.tools.compactionvalidator.schema.GeneratedSchema;
import org.apache.cassandra.tools.compactionvalidator.util.ByteUtil;
import org.apache.cassandra.tools.compactionvalidator.util.SeedUtil;
import org.apache.cassandra.tools.compactionvalidator.validation.MismatchReport;
import org.apache.cassandra.tools.compactionvalidator.validation.ValidationStats;

/**
 * Plain-text fallback for {@link ProgressTap} when the Lanterna TUI is disabled
 * or unavailable.  Writes one line per significant event to the given stream.
 *
 * <p>Periodic progress events are throttled to roughly once per second so that
 * the output remains scannable in a terminal or a log file.
 */
public class PlainTextReporter implements ProgressTap
{
    private static final long PROGRESS_THROTTLE_MS = 1000L;

    private final PrintStream out;

    private long lastDataGenLogMs = 0L;
    private long lastLegacyLogMs = 0L;
    private long lastCursorLogMs = 0L;
    private long lastValidationLogMs = 0L;

    // Most recent task batch info per backend (set by onCompactionTaskStart, read by
    // onCompactionProgress so the per-task line can show "task X of Y" instead of a
    // bare percentage). 0 = unknown.
    private int legacyTaskIndex = 0, legacyTasksInBatch = 0;
    private int cursorTaskIndex = 0, cursorTasksInBatch = 0;

    /**
     * Side display labels — populated by {@link #onComparisonLabels}. The plain-text
     * output uses these in place of the "legacy"/"cursor" routing tags so log lines
     * read like {@code [compact:control-stcs]} instead of {@code [compact:legacy]}
     * for runs that aren't comparing the legacy iterator vs cursor.
     */
    private String controlLabel = "legacy";
    private String experimentLabel = "cursor";

    public PlainTextReporter(PrintStream out)
    {
        this.out = out;
    }

    @Override
    public synchronized void onRunStart(int runNumber, long seed)
    {
        out.printf("[run %d] Starting run with seed=%s%n", runNumber, SeedUtil.toHex(seed));
    }

    @Override
    public synchronized void onComparisonLabels(String controlName, String experimentName)
    {
        if (controlName != null && !controlName.isEmpty()) controlLabel = controlName;
        if (experimentName != null && !experimentName.isEmpty()) experimentLabel = experimentName;
        out.printf("[run] comparison: control=%s  experiment=%s%n", controlLabel, experimentLabel);
    }

    /** Translates a routing tag ({@code "legacy"}/{@code "cursor"}) to the user-visible label. */
    private String labelFor(String backend)
    {
        if ("legacy".equals(backend))  return controlLabel;
        if ("cursor".equals(backend))  return experimentLabel;
        return backend;
    }

    @Override
    public synchronized void onSchemaReady(GeneratedSchema schema)
    {
        out.printf("[schema] keyspace=%s table=%s pk=%d ck=%d static=%d regular=%d%n",
                   schema.getKeyspaceName(), schema.getTableName(),
                   schema.getPartitionKeys().size(), schema.getClusteringKeys().size(),
                   schema.getStaticColumns().size(), schema.getRegularColumns().size());
    }

    @Override
    public synchronized void onDataGenProgress(long bytes, long target, long partitions, long rows, double bytesPerSec)
    {
        long now = System.currentTimeMillis();
        if (now - lastDataGenLogMs < PROGRESS_THROTTLE_MS)
            return;
        lastDataGenLogMs = now;
        double pct = target > 0 ? (100.0 * bytes / target) : 0.0;
        out.printf("[datagen] %s/%s (%.1f%%)  partitions=%d rows=%d  %.1f MiB/s%n",
                   ByteUtil.humanReadable(bytes), ByteUtil.humanReadable(target),
                   pct, partitions, rows, bytesPerSec / (1024.0 * 1024.0));
    }

    @Override
    public synchronized void onDataGenComplete(DataGenStats stats, long durationMs)
    {
        out.printf("[datagen] complete: %s in %.1fs (%.1f MiB/s)%n",
                   ByteUtil.humanReadable(stats.getBytesWritten()),
                   durationMs / 1000.0, stats.throughputMiBs());
    }

    @Override
    public synchronized void onCompactionProgress(String backend, long bytes, long target, long partitions,
                                                  double bytesPerSec, long currentTaskBytes, long currentTaskTotal)
    {
        long now = System.currentTimeMillis();
        long lastMs = "legacy".equals(backend) ? lastLegacyLogMs : lastCursorLogMs;
        if (now - lastMs < PROGRESS_THROTTLE_MS)
            return;
        if ("legacy".equals(backend))
            lastLegacyLogMs = now;
        else
            lastCursorLogMs = now;
        int taskPct = currentTaskTotal > 0 ? (int) ((currentTaskBytes * 100L) / currentTaskTotal) : 0;
        int taskIdx = "legacy".equals(backend) ? legacyTaskIndex : cursorTaskIndex;
        int taskTotal = "legacy".equals(backend) ? legacyTasksInBatch : cursorTasksInBatch;
        String taskStr = (taskIdx > 0 && taskTotal > 0)
                         ? String.format("task %d of %d (%d%%)", taskIdx, taskTotal, taskPct)
                         : String.format("task %d%%", taskPct);
        out.printf("[compact:%s] %s cumulative  partitions=%d  %.1f MiB/s  %s%n",
                   labelFor(backend), ByteUtil.humanReadable(bytes), partitions,
                   bytesPerSec / (1024.0 * 1024.0), taskStr);
    }

    @Override
    public synchronized void onCompactionTaskStart(String backend, java.util.List<SstableInfo> taskInputs,
                                                   int targetLevel, int taskIndex, int tasksInBatch, int totalTasksDone)
    {
        if ("legacy".equals(backend))
        {
            legacyTaskIndex = taskIndex;
            legacyTasksInBatch = tasksInBatch;
        }
        else if ("cursor".equals(backend))
        {
            cursorTaskIndex = taskIndex;
            cursorTasksInBatch = tasksInBatch;
        }
        long inputBytes = 0L;
        for (SstableInfo s : taskInputs) inputBytes += s.sizeBytes;
        out.printf("[compact:%s] task %d of %d started: %d input sstables, %s%n",
                   labelFor(backend), taskIndex, tasksInBatch, taskInputs.size(),
                   ByteUtil.humanReadable(inputBytes));
    }

    @Override
    public synchronized void onSstableEmitted(String backend, String filename, long sizeBytes, int level)
    {
        out.printf("[compact:%s] emitted %s (%s) level=%d%n",
                   labelFor(backend), filename, ByteUtil.humanReadable(sizeBytes), level);
    }

    @Override
    public synchronized void onCompactionComplete(String backend, CompactionStats stats)
    {
        out.printf("[compact:%s] complete: %.1fs (%.1f MiB/s, %d partitions)%n",
                   labelFor(backend), stats.durationMs / 1000.0, stats.throughputMiBs(), stats.partitionsProcessed);
    }

    @Override
    public synchronized void onCompactionStatus(String backend, String message)
    {
        if (message == null || message.isEmpty()) return;
        out.printf("[compact:%s] %s%n", labelFor(backend), message);
    }

    @Override
    public synchronized void onValidationProgress(long partitionsChecked)
    {
        long now = System.currentTimeMillis();
        if (now - lastValidationLogMs < PROGRESS_THROTTLE_MS)
            return;
        lastValidationLogMs = now;
        out.printf("[validate] partitions checked: %d%n", partitionsChecked);
    }

    @Override
    public synchronized void onValidationComplete(ValidationStats stats, boolean success)
    {
        out.printf("[validate] complete: %s (%d partitions, %d rows)%n",
                   success ? "PASS" : "FAIL", stats.partitionsChecked.get(), stats.rowsChecked.get());
        long totalErrata = stats.totalErrataOccurrences();
        if (totalErrata > 0)
        {
            // Loud, prominent block so the operator sees it even when grepping for "PASS".
            // We intentionally print these lines individually so each errata rule's hit
            // count is easy to filter / count across a soak log.
            out.println("[validate] ╔══════════════════════════════════════════════════════════════════════════╗");
            out.printf ("[validate] ║ ERRATA SUPPRESSED — %d partition(s) matched a known cursor-compaction bug ║%n", totalErrata);
            out.println("[validate] ╠══════════════════════════════════════════════════════════════════════════╣");
            for (java.util.Map.Entry<org.apache.cassandra.tools.compactionvalidator.validation.ErrataRule, java.util.concurrent.atomic.AtomicLong> e
                 : stats.errataOccurrences.entrySet())
            {
                long n = e.getValue().get();
                if (n == 0) continue;
                out.printf("[validate] ║  rule=%-26s  count=%d%n", e.getKey().cliName(), n);
                out.printf("[validate] ║     %s%n", e.getKey().description());
            }
            out.println("[validate] ╚══════════════════════════════════════════════════════════════════════════╝");
        }
    }

    @Override
    public synchronized void onRunComplete(RunResult result)
    {
        out.printf("[run %d] PASSED in %.1fs (cursor speedup %.2fx)%n",
                   result.runNumber, result.elapsedMs() / 1000.0, result.cursorSpeedup());
    }

    @Override
    public synchronized void onRunFailed(MismatchReport report, RunResult result)
    {
        out.println();
        out.println("================================================================================");
        out.printf("[run %d] FAILED at partition %s%n", result.runNumber, report.partitionKeyHex);
        if (report.description != null)
            out.println("  " + report.description);
        out.println("================================================================================");
        if (result.preservedDir != null)
            out.printf("Preserved at: %s%n", result.preservedDir);
        out.printf("Re-run with:  compaction-validator --seed %s --once --no-cleanup%n",
                   SeedUtil.toHex(result.seed));

        if (result.schemaCql != null && !result.schemaCql.isEmpty())
        {
            out.println();
            out.println("--- SCHEMA ---");
            out.println(result.schemaCql);
        }

        out.println();
        out.println("--- LEGACY OUTPUT ---");
        out.println(report.legacyDump != null ? report.legacyDump : "(empty)");
        out.println("--- CURSOR OUTPUT ---");
        out.println(report.cursorDump != null ? report.cursorDump : "(empty)");
        out.println("================================================================================");
    }
}
