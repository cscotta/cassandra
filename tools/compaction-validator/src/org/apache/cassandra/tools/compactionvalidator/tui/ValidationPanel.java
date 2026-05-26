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
 * Three-line panel showing per-backend validation progress with a sliding
 * cursor character moving across the active SSTable filename.
 */
public class ValidationPanel implements TuiPanel
{
    private static final String TITLE = "Validation";

    private long partitionsChecked = 0L;
    private long totalPartitions = 0L; // best-effort target from compaction stats
    private boolean complete = false;
    private boolean success = true;

    /** Number of output SSTables the validator is reading on each side. Set on CompactionComplete. */
    private int legacySstableCount = 0;
    private int cursorSstableCount = 0;

    /** Per-run side display labels — populated by {@link TuiEvent.ComparisonLabels}. */
    private String controlLabel = "LEGACY";
    private String experimentLabel = "CURSOR";

    @Override
    public void onEvent(TuiEvent event)
    {
        if (event instanceof TuiEvent.RunStart)
        {
            partitionsChecked = 0L;
            totalPartitions = 0L;
            complete = false;
            success = true;
            legacySstableCount = 0;
            cursorSstableCount = 0;
        }
        else if (event instanceof TuiEvent.ComparisonLabels cl)
        {
            // The bar labels track the user's per-run side names; uppercase to fit
            // the existing "LEGACY"/"CURSOR" visual convention.
            if (cl.controlName != null && !cl.controlName.isEmpty())
                controlLabel = cl.controlName.toUpperCase();
            if (cl.experimentName != null && !cl.experimentName.isEmpty())
                experimentLabel = cl.experimentName.toUpperCase();
        }
        else if (event instanceof TuiEvent.DataGenComplete dgc)
        {
            // The denominator for the validation progress bar is the count of UNIQUE
            // partitions written by the data generator. Compaction's
            // partitionsProcessed is the cumulative-across-passes count which is
            // ~100x larger under multi-pass UCS, and using it here made a
            // freshly-validated 4% bar correspond to "we've checked 200% of the
            // unique partition set" — wildly misleading. Pin to dataGen instead.
            if (dgc.stats != null)
                totalPartitions = Math.max(totalPartitions, dgc.stats.getPartitionsWritten());
        }
        else if (event instanceof TuiEvent.CompactionComplete cc)
        {
            // Fall-back denominator if DataGenComplete didn't set one yet.
            if (cc.stats != null)
            {
                if (totalPartitions == 0)
                    totalPartitions = cc.stats.partitionsProcessed;
                if ("legacy".equals(cc.backend))
                    legacySstableCount = cc.stats.sstablesOut;
                else if ("cursor".equals(cc.backend))
                    cursorSstableCount = cc.stats.sstablesOut;
            }
        }
        else if (event instanceof TuiEvent.ValidationProgress p)
        {
            partitionsChecked = p.partitionsChecked;
        }
        else if (event instanceof TuiEvent.ValidationComplete vc)
        {
            complete = true;
            success = vc.success;
            if (vc.stats != null)
                partitionsChecked = vc.stats.partitionsChecked.get();
        }
    }

    @Override
    public void render(TextGraphics g, TerminalPosition origin, TerminalSize size)
    {
        if (size.getRows() < 1 || size.getColumns() < 16)
            return;

        // Clear region
        g.setBackgroundColor(TextColor.ANSI.DEFAULT);
        g.setForegroundColor(TextColor.ANSI.DEFAULT);
        g.fillRectangle(origin, size, ' ');

        int row = origin.getRow();
        int col = origin.getColumn();
        int width = size.getColumns();
        int height = size.getRows();

        // Header
        g.setForegroundColor(TextColor.ANSI.WHITE);
        TuiUtil.drawSectionHeader(g, col, row, width, TITLE);

        if (height < 2)
            return;

        // Two progress bars under the header.  Each takes one row.
        // We split partitionsChecked between the two arbitrarily — they
        // verify the same partition set so identical progress is correct.
        renderBar(g, col, row + 1, width, controlLabel, legacySstableCount);
        if (height >= 3)
            renderBar(g, col, row + 2, width, experimentLabel, cursorSstableCount);
    }

    private void renderBar(TextGraphics g, int col, int row, int width, String label, int sstableCount)
    {
        if (width < 16)
            return;

        double fraction;
        if (complete)
            fraction = 1.0;
        else if (totalPartitions > 0)
            fraction = (double) partitionsChecked / totalPartitions;
        else
            fraction = 0.0;
        if (fraction < 0)
            fraction = 0;
        if (fraction > 1)
            fraction = 1;

        // Layout: "LEGACY  [████░░░░░░] 78% | 312K partitions | reading 4 sstables"
        String prefix = TuiUtil.pad(label, 7) + " ";
        String pctStr = String.format(" %d%% | %s partitions",
                                      (int) Math.round(fraction * 100),
                                      TuiUtil.formatCount(partitionsChecked));
        // Trailing suffix describes what's being read on this side. The validator merge-
        // iterates the entire output set for each backend simultaneously, so a single
        // "current file" indicator is misleading — the count of input sstables is what's
        // actually meaningful.
        String suffix;
        if (sstableCount <= 0)
            suffix = "";
        else if (complete)
            suffix = String.format(" | done (%d sstable%s)", sstableCount, sstableCount == 1 ? "" : "s");
        else
            suffix = String.format(" | reading %d sstable%s", sstableCount, sstableCount == 1 ? "" : "s");

        int barWidth = Math.max(8, Math.min(20, (width - prefix.length() - pctStr.length() - suffix.length() - 4)));
        if (barWidth < 4)
            barWidth = 4;

        // Print prefix
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(col, row, prefix, SGR.BOLD);
        int x = col + prefix.length();

        // Open bracket
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(x, row, "[");
        x += 1;

        // Bar
        TextColor barColor;
        if (complete && success)
            barColor = TextColor.ANSI.GREEN;
        else if (complete && !success)
            barColor = TextColor.ANSI.RED;
        else
            barColor = TextColor.ANSI.CYAN;
        g.setForegroundColor(barColor);
        g.putString(x, row, TuiUtil.progressBar(fraction, barWidth));
        x += barWidth;

        // Close bracket
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(x, row, "]");
        x += 1;

        // Pct + partitions text
        int rightEdge = col + width;
        int remaining = rightEdge - x;
        if (remaining <= 0)
            return;
        g.setForegroundColor(TextColor.ANSI.WHITE);
        String trimmedPct = TuiUtil.truncate(pctStr, remaining);
        g.putString(x, row, trimmedPct);
        x += trimmedPct.length();

        // Sstable-count suffix
        remaining = rightEdge - x;
        if (remaining < 4 || suffix.isEmpty())
            return;
        g.setForegroundColor(complete ? TextColor.ANSI.GREEN : TextColor.ANSI.BLACK_BRIGHT);
        g.putString(x, row, TuiUtil.truncate(suffix, remaining));
    }
}
