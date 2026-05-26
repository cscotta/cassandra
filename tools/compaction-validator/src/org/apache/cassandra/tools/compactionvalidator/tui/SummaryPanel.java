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

import java.util.ArrayList;
import java.util.List;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;

import org.apache.cassandra.tools.compactionvalidator.RunResult;
import org.apache.cassandra.tools.compactionvalidator.compaction.CompactionStats;
import org.apache.cassandra.tools.compactionvalidator.data.DataGenStats;
import org.apache.cassandra.tools.compactionvalidator.util.SeedUtil;
import org.apache.cassandra.tools.compactionvalidator.validation.MismatchReport;
import org.apache.cassandra.tools.compactionvalidator.validation.ValidationStats;

/**
 * Modal-style summary shown at the end of every run.  After a successful run
 * it lasts seven seconds and shows a green "PASSED" header; after a failed
 * run it stays on screen with a red banner and renders a detailed
 * inspection view (schema CQL + per-side partition dumps + re-run hint)
 * until the user dismisses it.
 *
 * <p>The failure view is taller than the screen for any non-trivial mismatch.
 * Scrolling is supported via {@link #scrollUp(int)} / {@link #scrollDown(int)}
 * / {@link #scrollHome()} / {@link #scrollEnd()}, which {@link TuiManager}
 * wires up to keyboard input.
 *
 * <p>The panel is "active" between {@link #activeUntilMs()} = displayedAtMs
 * and the auto-dismiss instant, after which {@link #isActive()} returns false
 * so {@link TuiManager} can resume the regular layout.
 */
public class SummaryPanel implements TuiPanel
{
    /** How long the success summary stays visible before auto-dismissing. */
    public static final long SUCCESS_DISPLAY_MS = 7_000L;

    private RunResult result;
    private MismatchReport report;
    private boolean active = false;
    private long displayedAtMs = 0L;
    private long countdownTotalMs = SUCCESS_DISPLAY_MS;

    /**
     * Vertical scroll offset within the body of the failure inspection view, in
     * lines. Updated by {@link #scrollUp}/{@link #scrollDown}; reset to 0 on
     * each {@link TuiEvent.RunFailed}. The render method clamps it so it can
     * never run past the last line of content.
     */
    private int scrollOffset = 0;

    /** Last total body-line count rendered, used to clamp {@link #scrollOffset}. */
    private int lastBodyLineCount = 0;

    /** Last body-window height, used by {@link #scrollEnd()} to compute the bottom. */
    private int lastBodyWindow = 0;

    @Override
    public void onEvent(TuiEvent event)
    {
        if (event instanceof TuiEvent.RunStart)
        {
            // Reset summary on new run start
            this.active = false;
            this.result = null;
            this.report = null;
            this.scrollOffset = 0;
        }
        else if (event instanceof TuiEvent.RunComplete c)
        {
            this.result = c.result;
            this.report = null;
            this.active = true;
            this.displayedAtMs = System.currentTimeMillis();
            this.countdownTotalMs = SUCCESS_DISPLAY_MS;
            this.scrollOffset = 0;
        }
        else if (event instanceof TuiEvent.RunFailed f)
        {
            this.result = f.result;
            this.report = f.report;
            this.active = true;
            this.displayedAtMs = System.currentTimeMillis();
            // Failure summary is sticky — countdown ignored
            this.countdownTotalMs = -1L;
            this.scrollOffset = 0;
        }
    }

    /** Returns whether this panel currently has content to display. */
    public boolean isActive()
    {
        if (!active)
            return false;
        if (countdownTotalMs < 0)
            return true; // failure summary is sticky
        return System.currentTimeMillis() - displayedAtMs < countdownTotalMs;
    }

    /** Dismisses an active summary so the manager can move on. */
    public void dismiss()
    {
        this.active = false;
    }

    /** @return whether the most recent summary represents a failed run */
    public boolean isFailure()
    {
        return result != null && !result.success;
    }

    @Override
    public void render(TextGraphics g, TerminalPosition origin, TerminalSize size)
    {
        if (!isActive())
            return;
        if (size.getRows() < 1 || size.getColumns() < 10)
            return;

        boolean failure = isFailure();
        TextColor bg = failure ? TextColor.ANSI.RED : TextColor.ANSI.DEFAULT;
        TextColor fg = failure ? TextColor.ANSI.WHITE : TextColor.ANSI.WHITE;

        // Clear region (with red background on failure)
        g.setBackgroundColor(bg);
        g.setForegroundColor(fg);
        g.fillRectangle(origin, size, ' ');

        List<String> lines = failure ? failureLines() : successLines();
        lastBodyLineCount = lines.size();

        int row = origin.getRow();
        int col = origin.getColumn();
        int width = size.getColumns();
        int height = size.getRows();

        // Header
        if (height >= 1)
        {
            g.setBackgroundColor(bg);
            g.setForegroundColor(failure ? TextColor.ANSI.WHITE : TextColor.ANSI.GREEN);
            String header = failure
                ? ("═══ FAILED — Run #" + (result == null ? "?" : result.runNumber)
                   + (lines.size() > height - 2 ? "  (↑/↓ PgUp/PgDn Home/End to scroll)" : "")
                   + " ═══")
                : ("─── Summary ");
            StringBuilder sb = new StringBuilder(width);
            sb.append(header);
            char fill = failure ? '═' : '─';
            while (sb.length() < width)
                sb.append(fill);
            if (sb.length() > width)
                sb.setLength(width);
            g.putString(col, row, sb.toString(), SGR.BOLD);
        }

        // Body window: start at row+1, height = remaining rows. Apply scroll offset
        // (clamped) so the content can scroll under the fixed header.
        int bodyTop = row + 1;
        int bodyHeight = Math.max(0, height - 1);
        lastBodyWindow = bodyHeight;
        if (bodyHeight == 0)
            return;

        int maxOffset = Math.max(0, lines.size() - bodyHeight);
        if (scrollOffset > maxOffset)
            scrollOffset = maxOffset;
        if (scrollOffset < 0)
            scrollOffset = 0;

        for (int i = 0; i < bodyHeight; i++)
        {
            int lineIdx = scrollOffset + i;
            if (lineIdx >= lines.size())
                break;
            String line = lines.get(lineIdx);
            // Section banners get a yellow accent so the eye can find them quickly while
            // scrolling. Everything else uses the panel's default fg/bg.
            boolean isBanner = line.startsWith("──") || line.startsWith("==");
            g.setBackgroundColor(bg);
            g.setForegroundColor(isBanner ? TextColor.ANSI.YELLOW : fg);
            g.putString(col, bodyTop + i, TuiUtil.truncate(line, width));
        }
    }

    private List<String> successLines()
    {
        List<String> out = new ArrayList<>();
        if (result == null)
            return out;
        out.add(String.format("Run #%d PASSED in %s",
                              result.runNumber,
                              TuiUtil.formatDuration(result.elapsedMs())));

        DataGenStats dg = result.dataGenStats;
        if (dg != null)
        {
            out.add(String.format("DataGen: %s | %s partitions | %s rows",
                                  TuiUtil.formatBytes(dg.getBytesWritten()),
                                  TuiUtil.formatCount(dg.getPartitionsWritten()),
                                  TuiUtil.formatCount(dg.getRowsWritten())));
        }

        CompactionStats ls = result.legacyStats;
        if (ls != null)
        {
            out.add(String.format("Legacy:  %.1fs | %.0f MiB/s",
                                  ls.durationMs / 1000.0, ls.throughputMiBs()));
        }

        CompactionStats cs = result.cursorStats;
        if (cs != null)
        {
            double speedup = result.cursorSpeedup();
            String star = speedup >= 1.05 ? " ★" : "";
            out.add(String.format("Cursor:  %.1fs | %.0f MiB/s | %.2fx speedup%s",
                                  cs.durationMs / 1000.0, cs.throughputMiBs(), speedup, star));
        }

        ValidationStats vs = result.validationStats;
        if (vs != null)
        {
            out.add(String.format("Validation: %s partitions matched",
                                  TuiUtil.formatCount(vs.partitionsChecked.get())));
        }

        long remainingMs = countdownTotalMs - (System.currentTimeMillis() - displayedAtMs);
        if (remainingMs < 0)
            remainingMs = 0;
        out.add(String.format("Next run in: %ds", (remainingMs + 999) / 1000));
        return out;
    }

    private List<String> failureLines()
    {
        List<String> out = new ArrayList<>();
        if (result == null)
        {
            out.add("Run failed (no detail available)");
            return out;
        }

        // === Top: identity / where to repro ===
        out.add(String.format("Seed: %s    Run #%d    Elapsed: %s",
                              SeedUtil.toHex(result.seed),
                              result.runNumber,
                              TuiUtil.formatDuration(result.elapsedMs())));

        if (report != null)
        {
            out.add("Mismatch in partition: " + report.partitionKeyHex);
            if (report.description != null)
                out.add("Detail: " + report.description);
            out.add(String.format("Partitions checked before failure: %d",
                                  report.partitionsCheckedBeforeFailure));
        }
        else if (result.failureDetail != null)
        {
            out.add("Failure: " + result.failureDetail);
        }

        if (result.preservedDir != null)
            out.add("Preserved at: " + result.preservedDir);

        out.add(String.format("Re-run: compaction-validator --seed %s --once --no-cleanup",
                              SeedUtil.toHex(result.seed)));
        out.add("");

        // === Schema overview ===
        out.add("══ Schema ══");
        out.add(String.format("%s.%s  (%d PK, %d CK, %d static, %d regular)",
                              result.keyspaceName, result.tableName,
                              result.partitionKeyCount, result.clusteringKeyCount,
                              result.staticColumnCount, result.regularColumnCount));
        out.add(String.format("UCS: target_sstable_size=%dMiB  base_shard_count=%d  compression_chunk=%dKiB",
                              result.targetSstableSizeMiB, result.baseShardCount,
                              result.compressionChunkKb));
        if (result.schemaCql != null && !result.schemaCql.isEmpty())
        {
            out.add("");
            for (String line : result.schemaCql.split("\\R"))
                out.add(line);
        }

        // === Per-side partition dumps ===
        if (report != null)
        {
            out.add("");
            out.add("══ Legacy partition contents ══");
            if (report.legacyDump != null && !report.legacyDump.isEmpty())
                for (String line : report.legacyDump.split("\\R"))
                    out.add(line);
            else
                out.add("(empty)");

            out.add("");
            out.add("══ Cursor partition contents ══");
            if (report.cursorDump != null && !report.cursorDump.isEmpty())
                for (String line : report.cursorDump.split("\\R"))
                    out.add(line);
            else
                out.add("(empty)");
        }

        out.add("");
        out.add("Press Q to quit, R to retry, N for next seed, ↑/↓ PgUp/PgDn Home/End to scroll");
        return out;
    }

    // -------------------------------------------------------------------------
    // Scroll API (driven by TuiManager keystroke handling)
    // -------------------------------------------------------------------------

    /** Scrolls the failure inspection view up by {@code n} lines. */
    public void scrollUp(int n)
    {
        scrollOffset = Math.max(0, scrollOffset - Math.max(1, n));
    }

    /**
     * Scrolls the failure inspection view down by {@code n} lines, clamped to
     * the last fully-visible window. The actual clamp happens in {@link #render}
     * because that's where we know the body window height; here we just advance
     * the offset and let the render clamp it down if needed.
     */
    public void scrollDown(int n)
    {
        scrollOffset = scrollOffset + Math.max(1, n);
    }

    /** Scrolls all the way to the top. */
    public void scrollHome()
    {
        scrollOffset = 0;
    }

    /**
     * Scrolls all the way to the bottom. Uses the most recent
     * {@link #lastBodyLineCount} / {@link #lastBodyWindow} captured during
     * {@link #render}, so the first call right after a failure may land short
     * of the actual end if no frame has been rendered yet — render() will then
     * clamp the offset on the next frame, which lands us at the correct spot.
     */
    public void scrollEnd()
    {
        scrollOffset = Math.max(0, lastBodyLineCount - Math.max(1, lastBodyWindow));
    }
}
