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
package org.apache.cassandra.tools.compactionvalidator.tui;

import java.util.HashSet;
import java.util.Set;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;

/**
 * Side-by-side compaction visualisation: a {@link LsmTreeWidget} for each of
 * the legacy and cursor backends, plus a throughput readout under each.
 *
 * <p>The {@code ✦} active marker pulses at ~2 Hz (visible / hidden) while
 * a backend is producing output.
 */
public class CompactionPanel implements TuiPanel
{
    private static final String BACKEND_LEGACY = "legacy";
    private static final String BACKEND_CURSOR = "cursor";
    private static final String TITLE = "Compaction";
    private static final long PULSE_PERIOD_MS = 500L;

    private final LsmTreeWidget legacy = new LsmTreeWidget();
    private final LsmTreeWidget cursor = new LsmTreeWidget();

    private long legacyBytes = 0L;
    private long legacyTarget = 0L;
    private long legacyParts = 0L;
    private double legacyBytesPerSec = 0.0;
    private long legacyStartNanos = 0L;
    private long legacyFrozenDurationMs = 0L;
    private boolean legacyComplete = false;
    /** Per-task progress fields — used for the per-task 0..100% bar so multi-pass UCS doesn't overrun. */
    private long legacyCurTaskBytes = 0L;
    private long legacyCurTaskTotal = 0L;

    private long cursorBytes = 0L;
    private long cursorTarget = 0L;
    private long cursorParts = 0L;
    private double cursorBytesPerSec = 0.0;
    private long cursorStartNanos = 0L;
    private long cursorFrozenDurationMs = 0L;
    private boolean cursorComplete = false;
    private long cursorCurTaskBytes = 0L;
    private long cursorCurTaskTotal = 0L;

    /** Most recent task description, per backend, shown as a status line under the LSM widget. */
    private String legacyTaskStatus = "";
    private String cursorTaskStatus = "";
    /**
     * Counter components rendered as {@code "Done X/Y"} with an optional {@code "(N running)"}
     * suffix when more than one task is in flight (which happens under
     * {@code --compaction-threads N > 1}).
     *
     * <p>{@code tasksInBatch} comes from {@code CompactionTaskStart};
     * {@code tasksDone} is incremented locally on {@code CompactionTaskEnd}; the in-flight
     * count is the size of the in-flight set tracked here (one entry per running task,
     * keyed by {@code taskIndex} so a {@code CompactionTaskEnd} for task N can remove the
     * right entry even if tasks complete out of start order).
     *
     * <p>The wire event's {@code totalTasksDone} field is the count BEFORE the in-flight task
     * started (because {@link org.apache.cassandra.db.compaction.CompactionDriver} increments
     * after the task returns), so we don't read it directly — we mirror the increment locally
     * on each {@code TaskEnd} and a final snap on {@code CompactionComplete} brings the
     * counter to {@code tasksInBatch} even if some events were lost on the bus.
     */
    private int legacyTasksInBatch = 0, legacyTasksDone = 0;
    private int cursorTasksInBatch = 0, cursorTasksDone = 0;
    private final Set<Integer> legacyInFlight = new HashSet<>();
    private final Set<Integer> cursorInFlight = new HashSet<>();

    /**
     * Display labels for the two sides — populated by {@link TuiEvent.ComparisonLabels}
     * once per run. Fall back to capitalised default tags if the event never fires
     * (e.g. legacy code paths that bypass {@code ProgressTap.onComparisonLabels}).
     */
    private String controlLabel = "LEGACY";
    private String experimentLabel = "CURSOR";

    @Override
    public void onEvent(TuiEvent event)
    {
        if (event instanceof TuiEvent.RunStart)
        {
            legacy.clear();
            cursor.clear();
            legacyBytes = legacyParts = legacyTarget = 0L;
            legacyBytesPerSec = 0.0;
            legacyStartNanos = 0L;
            legacyFrozenDurationMs = 0L;
            legacyComplete = false;
            legacyTaskStatus = "";
            legacyTasksInBatch = legacyTasksDone = 0;
            legacyInFlight.clear();
            legacyCurTaskBytes = legacyCurTaskTotal = 0L;
            cursorBytes = cursorParts = cursorTarget = 0L;
            cursorBytesPerSec = 0.0;
            cursorStartNanos = 0L;
            cursorFrozenDurationMs = 0L;
            cursorComplete = false;
            cursorTaskStatus = "";
            cursorTasksInBatch = cursorTasksDone = 0;
            cursorInFlight.clear();
            cursorCurTaskBytes = cursorCurTaskTotal = 0L;
        }
        else if (event instanceof TuiEvent.ComparisonLabels cl)
        {
            // Capture the per-run side names so renderBackend can show config-driven
            // labels instead of hardcoded "LEGACY ITERATOR"/"CURSOR".
            if (cl.controlName != null && !cl.controlName.isEmpty())
                controlLabel = cl.controlName.toUpperCase();
            if (cl.experimentName != null && !cl.experimentName.isEmpty())
                experimentLabel = cl.experimentName.toUpperCase();
        }
        else if (event instanceof TuiEvent.CompactionInputs e)
        {
            // Pre-populate the LSM widget with the input set so it doesn't say
            // "(no SSTables yet)" while the first task is still scanning.
            LsmTreeWidget target = BACKEND_LEGACY.equals(e.backend) ? legacy
                                : BACKEND_CURSOR.equals(e.backend) ? cursor : null;
            if (target != null)
            {
                for (org.apache.cassandra.tools.compactionvalidator.SstableInfo s : e.inputs)
                    target.addSstable(s.filename, s.sizeBytes, s.level);
            }
        }
        else if (event instanceof TuiEvent.CompactionTaskStart e)
        {
            String status = describeTaskStart(e);
            if (BACKEND_LEGACY.equals(e.backend))
            {
                legacyTaskStatus = status;
                legacyTasksInBatch = e.tasksInBatch;
                legacyInFlight.add(e.taskIndex);
                // With taskConcurrency=1 there's only one task in flight, so the per-side
                // bar reads the new task's input total. With taskConcurrency>1 the driver
                // sends the SUM of in-flight task bytes in CompactionProgress.currentTaskTotal,
                // and that overwrites our local read on the next tick (see the progress branch
                // below). The local set here is purely a starting value until the first tick.
                legacyCurTaskBytes = 0L;
                legacyCurTaskTotal = sumInputBytes(e.taskInputs);
            }
            else if (BACKEND_CURSOR.equals(e.backend))
            {
                cursorTaskStatus = status;
                cursorTasksInBatch = e.tasksInBatch;
                cursorInFlight.add(e.taskIndex);
                cursorCurTaskBytes = 0L;
                cursorCurTaskTotal = sumInputBytes(e.taskInputs);
            }
        }
        else if (event instanceof TuiEvent.CompactionTaskEnd e)
        {
            String status = describeTaskEnd(e);
            if (BACKEND_LEGACY.equals(e.backend))
            {
                legacyTaskStatus = status;
                // Remove the just-finished task from the in-flight set; if it wasn't
                // there (e.g. a TaskStart was dropped on the bus) the remove is a no-op.
                legacyInFlight.remove(e.taskIndex);
                legacyTasksDone++;
                // Clear per-task progress only when no tasks remain in flight. With
                // taskConcurrency>1, other tasks are still running and their aggregate
                // progress should keep showing. With =1 this matches the previous behaviour.
                if (legacyInFlight.isEmpty())
                {
                    legacyCurTaskBytes = 0L;
                    legacyCurTaskTotal = 0L;
                }
            }
            else if (BACKEND_CURSOR.equals(e.backend))
            {
                cursorTaskStatus = status;
                cursorInFlight.remove(e.taskIndex);
                cursorTasksDone++;
                if (cursorInFlight.isEmpty())
                {
                    cursorCurTaskBytes = 0L;
                    cursorCurTaskTotal = 0L;
                }
            }
        }
        else if (event instanceof TuiEvent.SstableEmitted e)
        {
            if (BACKEND_LEGACY.equals(e.backend))
                legacy.addSstable(e.filename, e.sizeBytes, e.level);
            else if (BACKEND_CURSOR.equals(e.backend))
                cursor.addSstable(e.filename, e.sizeBytes, e.level);
        }
        else if (event instanceof TuiEvent.CompactionProgress p)
        {
            if (BACKEND_LEGACY.equals(p.backend))
            {
                if (legacyStartNanos == 0L)
                    legacyStartNanos = System.nanoTime();
                legacyBytes = p.bytes;
                legacyTarget = p.target;
                legacyParts = p.partitions;
                legacyBytesPerSec = p.bytesPerSec;
                legacyCurTaskBytes = p.currentTaskBytes;
                if (p.currentTaskTotal > 0)
                    legacyCurTaskTotal = p.currentTaskTotal;
                legacy.markActive();
            }
            else if (BACKEND_CURSOR.equals(p.backend))
            {
                if (cursorStartNanos == 0L)
                    cursorStartNanos = System.nanoTime();
                cursorBytes = p.bytes;
                cursorTarget = p.target;
                cursorParts = p.partitions;
                cursorBytesPerSec = p.bytesPerSec;
                cursorCurTaskBytes = p.currentTaskBytes;
                if (p.currentTaskTotal > 0)
                    cursorCurTaskTotal = p.currentTaskTotal;
                cursor.markActive();
            }
        }
        else if (event instanceof TuiEvent.CompactionComplete c)
        {
            if (BACKEND_LEGACY.equals(c.backend))
            {
                legacyComplete = true;
                legacy.markInactive();
                // Final snap: if any TaskEnd events were lost between the driver and the
                // bus (or the run ended via the maximal pass without firing a per-task End),
                // the visible counter would still read N-1. Force it to the batch total so
                // the user sees a consistent "Done M/M" once the backend is green, and clear
                // the in-flight set so the "(N running)" suffix doesn't linger.
                if (legacyTasksInBatch > 0)
                    legacyTasksDone = legacyTasksInBatch;
                legacyInFlight.clear();
                legacyCurTaskBytes = 0L;
                legacyCurTaskTotal = 0L;
                if (c.stats != null)
                {
                    legacyBytes = c.stats.bytesProcessed;
                    legacyParts = c.stats.partitionsProcessed;
                    if (c.stats.durationMs > 0)
                    {
                        legacyBytesPerSec = (legacyBytes * 1000.0) / c.stats.durationMs;
                        legacyFrozenDurationMs = c.stats.durationMs;
                    }
                }
                if (legacyFrozenDurationMs == 0L)
                    legacyFrozenDurationMs = legacyStartNanos > 0
                                             ? Math.max(1L, (System.nanoTime() - legacyStartNanos) / 1_000_000L)
                                             : 1L;
            }
            else if (BACKEND_CURSOR.equals(c.backend))
            {
                cursorComplete = true;
                cursor.markInactive();
                if (cursorTasksInBatch > 0)
                    cursorTasksDone = cursorTasksInBatch;
                cursorInFlight.clear();
                cursorCurTaskBytes = 0L;
                cursorCurTaskTotal = 0L;
                if (c.stats != null)
                {
                    cursorBytes = c.stats.bytesProcessed;
                    cursorParts = c.stats.partitionsProcessed;
                    if (c.stats.durationMs > 0)
                    {
                        cursorBytesPerSec = (cursorBytes * 1000.0) / c.stats.durationMs;
                        cursorFrozenDurationMs = c.stats.durationMs;
                    }
                }
                if (cursorFrozenDurationMs == 0L)
                    cursorFrozenDurationMs = cursorStartNanos > 0
                                             ? Math.max(1L, (System.nanoTime() - cursorStartNanos) / 1_000_000L)
                                             : 1L;
            }
        }
    }

    @Override
    public void render(TextGraphics g, TerminalPosition origin, TerminalSize size)
    {
        if (size.getRows() < 2 || size.getColumns() < 16)
            return;

        // Clear region
        g.setBackgroundColor(TextColor.ANSI.DEFAULT);
        g.setForegroundColor(TextColor.ANSI.DEFAULT);
        g.fillRectangle(origin, size, ' ');

        int row = origin.getRow();
        int col = origin.getColumn();
        int width = size.getColumns();
        int height = size.getRows();

        // Outer header
        g.setForegroundColor(TextColor.ANSI.WHITE);
        TuiUtil.drawSectionHeader(g, col, row, width, TITLE);

        if (height < 3)
            return;

        // Two side-by-side columns
        int innerRow = row + 1;
        int innerHeight = height - 1;
        int gutter = 2;
        int colWidth = (width - gutter) / 2;
        if (colWidth < 8)
        {
            // Too narrow for two columns — stack vertically
            int halfHeight = innerHeight / 2;
            renderBackend(g, controlLabel, legacy, legacyBytes, legacyTarget, legacyParts,
                          legacyBytesPerSec, legacyStartNanos, legacyFrozenDurationMs, legacyComplete,
                          legacyTaskStatus, formatTaskCounter(legacyTasksInBatch, legacyTasksDone, legacyInFlight.size()),
                          legacyCurTaskBytes, legacyCurTaskTotal,
                          new TerminalPosition(col, innerRow), new TerminalSize(width, halfHeight));
            renderBackend(g, experimentLabel, cursor, cursorBytes, cursorTarget, cursorParts,
                          cursorBytesPerSec, cursorStartNanos, cursorFrozenDurationMs, cursorComplete,
                          cursorTaskStatus, formatTaskCounter(cursorTasksInBatch, cursorTasksDone, cursorInFlight.size()),
                          cursorCurTaskBytes, cursorCurTaskTotal,
                          new TerminalPosition(col, innerRow + halfHeight),
                          new TerminalSize(width, innerHeight - halfHeight));
            return;
        }

        renderBackend(g, controlLabel, legacy, legacyBytes, legacyTarget, legacyParts,
                      legacyBytesPerSec, legacyStartNanos, legacyFrozenDurationMs, legacyComplete,
                      legacyTaskStatus, formatTaskCounter(legacyTasksInBatch, legacyTasksDone, legacyInFlight.size()),
                      legacyCurTaskBytes, legacyCurTaskTotal,
                      new TerminalPosition(col, innerRow),
                      new TerminalSize(colWidth, innerHeight));
        renderBackend(g, experimentLabel, cursor, cursorBytes, cursorTarget, cursorParts,
                      cursorBytesPerSec, cursorStartNanos, cursorFrozenDurationMs, cursorComplete,
                      cursorTaskStatus, formatTaskCounter(cursorTasksInBatch, cursorTasksDone, cursorInFlight.size()),
                      cursorCurTaskBytes, cursorCurTaskTotal,
                      new TerminalPosition(col + colWidth + gutter, innerRow),
                      new TerminalSize(width - colWidth - gutter, innerHeight));
    }

    /**
     * Formats the counter line shown under each backend's bar.
     *
     * <p>Sequential mode ({@code inFlight ≤ 1}) renders as {@code "Done X/Y"}; parallel
     * mode appends a {@code "(N running)"} suffix so the operator can see at a glance
     * how many tasks are currently in flight under {@code --compaction-threads N>1}.
     *
     * <p>Returns an empty string when no task has been seen yet (avoids rendering a
     * stub like {@code "Done 0/0"} before the first {@code CompactionTaskStart}).
     */
    private static String formatTaskCounter(int tasksInBatch, int tasksDone, int inFlight)
    {
        if (tasksInBatch <= 0)
            return "";
        if (inFlight > 1)
            return String.format("Done %d/%d  (%d running)", tasksDone, tasksInBatch, inFlight);
        return String.format("Done %d/%d", tasksDone, tasksInBatch);
    }

    private void renderBackend(TextGraphics g, String label, LsmTreeWidget widget,
                               long bytes, long target, long partitions, double bytesPerSec,
                               long startNanos, long frozenDurationMs,
                               boolean complete,
                               String taskStatus, String taskCounter,
                               long curTaskBytes, long curTaskTotal,
                               TerminalPosition origin, TerminalSize size)
    {
        if (size.getRows() < 1 || size.getColumns() < 4)
            return;

        int row = origin.getRow();
        int col = origin.getColumn();
        int width = size.getColumns();
        int height = size.getRows();

        // Top border line with title
        g.setForegroundColor(TextColor.ANSI.WHITE);
        StringBuilder top = new StringBuilder(width);
        top.append("┌─── ").append(label).append(' ');
        while (top.length() < width - 1)
            top.append('─');
        if (top.length() < width)
            top.append('┐');
        if (top.length() > width)
            top.setLength(width);
        g.putString(col, row, top.toString());

        if (height < 2)
            return;

        // Active pulse marker
        boolean pulseOn = (System.currentTimeMillis() / PULSE_PERIOD_MS) % 2 == 0;
        boolean active = widget.isActive() && !complete && pulseOn;

        // LSM widget body
        int bodyTop = row + 1;
        // Reserve room for: status row + progress bar + throughput row + bottom border = 4 rows
        int bodyHeight = Math.max(0, height - 5);
        if (bodyHeight > 0)
        {
            // Left border + LSM widget + right border
            for (int rr = 0; rr < bodyHeight; rr++)
            {
                g.setForegroundColor(TextColor.ANSI.WHITE);
                g.putString(col, bodyTop + rr, "│");
                if (width > 1)
                    g.putString(col + width - 1, bodyTop + rr, "│");
            }
            int innerCol = col + 1;
            int innerWidth = Math.max(0, width - 2);
            widget.render(g, new TerminalPosition(innerCol, bodyTop),
                          new TerminalSize(innerWidth, bodyHeight));

            // Active marker on first body row
            if (active && innerWidth >= 2)
            {
                g.setForegroundColor(TextColor.ANSI.MAGENTA);
                g.putString(col + width - 2, bodyTop, "✦", SGR.BOLD);
            }
        }

        // Status row (4th from bottom): describes the in-flight (or just-completed) task,
        // plus the "Task N/M  done: K" counter aligned right when there's room.
        if (height >= 5)
        {
            int statusRow = row + height - 4;
            g.setForegroundColor(TextColor.ANSI.WHITE);
            g.putString(col, statusRow, "│");
            if (width > 1)
                g.putString(col + width - 1, statusRow, "│");
            int innerWidth = Math.max(0, width - 2);
            String status = (taskStatus == null || taskStatus.isEmpty())
                            ? (complete ? " idle" : " preparing…")
                            : (" " + taskStatus);
            g.setForegroundColor(complete ? TextColor.ANSI.BLACK_BRIGHT : TextColor.ANSI.YELLOW);
            // The status row used to also show "Done X/Y (N running)" right-aligned.
            // The LSM widget + progress-bar already convey progress, so the counter
            // was visual noise; dropped per user feedback.
            g.putString(col + 1, statusRow, TuiUtil.truncate(status, innerWidth));
        }

        // Progress bar row (3rd from bottom): per-task progress (0..100%). Uses the in-flight
        // task's bytesScanned/inputTotal so the bar resets between tasks. The cumulative-vs-
        // initial-input fraction would exceed 100% under multi-pass strategies (UCS does
        // multiple rounds where each round's output becomes the next round's input).
        if (height >= 4)
        {
            int barRow = row + height - 3;
            g.setForegroundColor(TextColor.ANSI.WHITE);
            g.putString(col, barRow, "│");
            if (width > 1)
                g.putString(col + width - 1, barRow, "│");

            int innerWidth = Math.max(0, width - 2);
            double fraction = curTaskTotal > 0 ? (double) curTaskBytes / curTaskTotal : 0.0;
            double fractionClamped = Math.max(0.0, Math.min(1.0, fraction));

            String suffix = curTaskTotal > 0
                            ? String.format(" %s/%s (%d%%) ",
                                            TuiUtil.formatBytes(curTaskBytes),
                                            TuiUtil.formatBytes(curTaskTotal),
                                            (int) (fractionClamped * 100))
                            : (complete ? "  done  " : "  idle  ");
            int suffixLen = suffix.length();
            int barAvailable = Math.max(2, innerWidth - suffixLen - 2); // 2 = '[' + ']'

            if (innerWidth >= barAvailable + suffixLen + 2)
            {
                g.setForegroundColor(TextColor.ANSI.WHITE);
                g.putString(col + 1, barRow, "[");
                g.setForegroundColor(complete ? TextColor.ANSI.GREEN : TextColor.ANSI.YELLOW);
                g.putString(col + 2, barRow, TuiUtil.progressBar(fractionClamped, barAvailable));
                g.setForegroundColor(TextColor.ANSI.WHITE);
                g.putString(col + 2 + barAvailable, barRow, "]");
                g.setForegroundColor(complete ? TextColor.ANSI.GREEN : TextColor.ANSI.CYAN);
                g.putString(col + 3 + barAvailable, barRow, TuiUtil.truncate(suffix, suffixLen));
            }
            else if (innerWidth >= 4)
            {
                // Very narrow: just print bytes
                g.setForegroundColor(TextColor.ANSI.CYAN);
                g.putString(col + 1, barRow, TuiUtil.truncate(suffix.trim(), innerWidth));
            }
        }

        // Throughput line (penultimate row)
        if (height >= 3)
        {
            int throughputRow = row + height - 2;
            g.setForegroundColor(TextColor.ANSI.WHITE);
            g.putString(col, throughputRow, "│");
            if (width > 1)
                g.putString(col + width - 1, throughputRow, "│");

            double mibPerSec = bytesPerSec / (1024.0 * 1024.0);
            // partitions/s is computed from cumulative count + elapsed time. Elapsed time is
            // frozen on CompactionComplete (using stats.durationMs), so the rate stops drifting
            // after the phase finishes, mirroring the DataGenPanel pattern.
            long elapsedMs = frozenDurationMs > 0
                             ? frozenDurationMs
                             : (startNanos > 0 ? (System.nanoTime() - startNanos) / 1_000_000L : 0L);
            double partsPerSec = elapsedMs > 0 ? (partitions * 1000.0) / elapsedMs : 0.0;

            // The "scanned" partition count is cumulative across UCS multi-pass rounds —
            // the same partition is read once per round it appears in, and a 132-pass run
            // over 1.4M unique partitions reports ~185M scanned. The label says "scanned"
            // (not "total") so the user reads this as "work done", not "unique partitions".
            String throughput = String.format(" %s p/s | %.0f MiB/s | %s scanned | %d sst ",
                                              TuiUtil.formatCount((long) partsPerSec),
                                              mibPerSec,
                                              TuiUtil.formatCount(partitions),
                                              widget.sstableCount());
            g.setForegroundColor(complete ? TextColor.ANSI.GREEN : TextColor.ANSI.CYAN);
            int innerWidth = Math.max(0, width - 2);
            g.putString(col + 1, throughputRow, TuiUtil.truncate(throughput, innerWidth), SGR.BOLD);
        }

        // Bottom border line
        if (height >= 2)
        {
            int bottomRow = row + height - 1;
            g.setForegroundColor(TextColor.ANSI.WHITE);
            StringBuilder bot = new StringBuilder(width);
            bot.append('└');
            while (bot.length() < width - 1)
                bot.append('─');
            if (bot.length() < width)
                bot.append('┘');
            if (bot.length() > width)
                bot.setLength(width);
            g.putString(col, bottomRow, bot.toString());
        }
    }

    /** Sums the on-disk sizes of a task's input set; used as the per-task bar denominator. */
    private static long sumInputBytes(java.util.List<org.apache.cassandra.tools.compactionvalidator.SstableInfo> inputs)
    {
        if (inputs == null) return 0L;
        long total = 0L;
        for (org.apache.cassandra.tools.compactionvalidator.SstableInfo s : inputs)
            total += s.sizeBytes;
        return total;
    }

    /** Builds the status string for the start of a task (e.g. "Compacting 4 sstables (256M, L0) → L0"). */
    private static String describeTaskStart(TuiEvent.CompactionTaskStart e)
    {
        java.util.List<org.apache.cassandra.tools.compactionvalidator.SstableInfo> ins = e.taskInputs;
        if (ins == null || ins.isEmpty())
            return "Compacting (no inputs)";
        long bytes = 0L;
        java.util.Set<Integer> levels = new java.util.TreeSet<>();
        for (org.apache.cassandra.tools.compactionvalidator.SstableInfo s : ins)
        {
            bytes += s.sizeBytes;
            levels.add(s.level);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Compacting ").append(ins.size()).append(" sstables (")
          .append(TuiUtil.formatBytes(bytes)).append(", ");
        if (levels.size() == 1)
            sb.append("L").append(levels.iterator().next());
        else
            sb.append("L").append(levels.iterator().next()).append("..L")
              .append(((java.util.TreeSet<Integer>) levels).last());
        sb.append(')');
        if (e.targetLevel >= 0)
            sb.append(" → L").append(e.targetLevel);
        return sb.toString();
    }

    /** Builds the status string for the end of a task (e.g. "Wrote 2 sstables (210M)"). */
    private static String describeTaskEnd(TuiEvent.CompactionTaskEnd e)
    {
        java.util.List<org.apache.cassandra.tools.compactionvalidator.SstableInfo> outs = e.taskOutputs;
        if (outs == null || outs.isEmpty())
            return "Task complete (no output)";
        long bytes = 0L;
        for (org.apache.cassandra.tools.compactionvalidator.SstableInfo s : outs)
            bytes += s.sizeBytes;
        return "Wrote " + outs.size() + " sstables (" + TuiUtil.formatBytes(bytes) + ")";
    }
}
