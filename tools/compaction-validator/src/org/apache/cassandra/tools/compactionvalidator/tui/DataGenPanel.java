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

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;

/**
 * Four-line panel showing data-generation progress: a unicode progress bar,
 * a counter row, and a rate row.  Updates on every
 * {@link TuiEvent.DataGenProgress} and is reset on {@link TuiEvent.RunStart}.
 */
public class DataGenPanel implements TuiPanel
{
    private static final String TITLE = "Data Generation";

    private long bytes = 0L;
    private long target = 0L;
    private long partitions = 0L;
    private long rows = 0L;
    private double bytesPerSec = 0.0;
    private long startNanos = 0L;
    private long frozenDurationMs = 0L;  // set on DataGenComplete; > 0 freezes the rate row
    private boolean complete = false;

    @Override
    public void onEvent(TuiEvent event)
    {
        if (event instanceof TuiEvent.RunStart)
        {
            this.bytes = 0L;
            this.target = 0L;
            this.partitions = 0L;
            this.rows = 0L;
            this.bytesPerSec = 0.0;
            this.startNanos = System.nanoTime();
            this.frozenDurationMs = 0L;
            this.complete = false;
        }
        else if (event instanceof TuiEvent.DataGenProgress p)
        {
            if (this.startNanos == 0L)
                this.startNanos = System.nanoTime();
            this.bytes = p.bytes;
            this.target = p.target;
            this.partitions = p.partitions;
            this.rows = p.rows;
            this.bytesPerSec = p.bytesPerSec;
        }
        else if (event instanceof TuiEvent.DataGenComplete c)
        {
            this.complete = true;
            if (c.stats != null)
            {
                this.bytes = c.stats.getBytesWritten();
                this.partitions = c.stats.getPartitionsWritten();
                this.rows = c.stats.getRowsWritten();
                if (c.durationMs > 0)
                    this.bytesPerSec = (this.bytes * 1000.0) / c.durationMs;
            }
            // Freeze elapsed time so partitions/s and rows/s stop drifting after the phase ends.
            // Prefer the authoritative durationMs from the stats; fall back to wall-clock if absent.
            if (c.durationMs > 0)
                this.frozenDurationMs = c.durationMs;
            else if (this.startNanos > 0)
                this.frozenDurationMs = Math.max(1L, (System.nanoTime() - this.startNanos) / 1_000_000L);
            else
                this.frozenDurationMs = 1L;
        }
    }

    @Override
    public void render(TextGraphics g, TerminalPosition origin, TerminalSize size)
    {
        if (size.getRows() < 1 || size.getColumns() < 10)
            return;

        // Clear region
        g.setBackgroundColor(TextColor.ANSI.DEFAULT);
        g.setForegroundColor(TextColor.ANSI.DEFAULT);
        g.fillRectangle(origin, size, ' ');

        int row = origin.getRow();
        int col = origin.getColumn();
        int width = size.getColumns();
        int height = size.getRows();

        // Section header
        g.setForegroundColor(TextColor.ANSI.WHITE);
        TuiUtil.drawSectionHeader(g, col, row, width, TITLE);

        if (height < 2)
            return;

        // Progress bar row (row + 1)
        double fraction = (target > 0) ? (double) bytes / target : 0.0;
        if (fraction < 0)
            fraction = 0;
        if (fraction > 1)
            fraction = 1;

        // Reserve space for the suffix " 4.7/10.0 GiB    ETA: 00:00:23"
        String bytesText = TuiUtil.formatBytes(bytes) + "/" + TuiUtil.formatBytes(Math.max(target, bytes));
        long etaMs = computeEtaMs();
        String etaText = etaMs < 0 ? "ETA: --:--" : ("ETA: " + TuiUtil.formatDuration(etaMs));
        String suffix = " " + bytesText + "    " + etaText;

        int barAvailable = Math.max(4, width - suffix.length() - 2);
        // Draw '[' bar ']' suffix
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(col, row + 1, "[");

        TextColor barColor = chooseBarColor();
        g.setForegroundColor(barColor);
        String bar = TuiUtil.progressBar(fraction, barAvailable);
        g.putString(col + 1, row + 1, bar);

        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(col + 1 + barAvailable, row + 1, "]");

        int suffixCol = col + 2 + barAvailable;
        int remaining = origin.getColumn() + width - suffixCol;
        if (remaining > 0)
        {
            g.setForegroundColor(TextColor.ANSI.WHITE);
            g.putString(suffixCol, row + 1, TuiUtil.truncate(suffix, remaining));
        }

        if (height < 3)
            return;

        // Counter row (row + 2)
        long avgBytesPerRow = rows > 0 ? bytes / rows : 0L;
        String counters = String.format("%s partitions | %s rows | avg %s/row",
                                        TuiUtil.formatCount(partitions),
                                        TuiUtil.formatCount(rows),
                                        TuiUtil.formatBytes(avgBytesPerRow));
        g.setForegroundColor(TextColor.ANSI.CYAN);
        g.putString(col, row + 2, TuiUtil.truncate(counters, width));

        if (height < 4)
            return;

        // Rate row (row + 3)
        double mibPerSec = bytesPerSec / (1024.0 * 1024.0);
        // Use the frozen final duration once the phase is complete; otherwise track wall clock.
        long elapsedMs = frozenDurationMs > 0
                         ? frozenDurationMs
                         : (startNanos > 0 ? (System.nanoTime() - startNanos) / 1_000_000L : 0L);
        double partsPerSec = (elapsedMs > 0) ? (partitions * 1000.0) / elapsedMs : 0.0;
        double rowsPerSec = (elapsedMs > 0) ? (rows * 1000.0) / elapsedMs : 0.0;
        String rateLine = String.format("%.1f MiB/s | %s partitions/s | %s rows/s",
                                        mibPerSec,
                                        TuiUtil.formatCount((long) partsPerSec),
                                        TuiUtil.formatCount((long) rowsPerSec));
        g.setForegroundColor(complete ? TextColor.ANSI.GREEN : TextColor.ANSI.YELLOW);
        g.putString(col, row + 3, TuiUtil.truncate(rateLine, width), SGR.BOLD);
    }

    private TextColor chooseBarColor()
    {
        if (complete)
            return TextColor.ANSI.GREEN;
        // Yellow if rate is below 50 MiB/s, green otherwise
        double mibPerSec = bytesPerSec / (1024.0 * 1024.0);
        if (mibPerSec < 50.0 && bytesPerSec > 0)
            return TextColor.ANSI.YELLOW;
        return TextColor.ANSI.GREEN;
    }

    private long computeEtaMs()
    {
        if (complete)
            return 0L;
        if (target <= 0 || bytesPerSec <= 0 || bytes >= target)
            return -1L;
        long remaining = target - bytes;
        return (long) (remaining * 1000.0 / bytesPerSec);
    }
}
