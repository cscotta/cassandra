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

import org.apache.cassandra.tools.compactionvalidator.util.SeedUtil;

/**
 * Single-line header panel showing the run number, seed, current phase, and
 * elapsed wall-clock time.  Always rendered on row 0.
 *
 * <p>Layout (one row):
 * <pre>
 * Cassandra Compaction Validator | Run #N | seed=0x... | Phase: [DATA_GEN] | Elapsed: 00:01:23
 * </pre>
 */
public class HeaderPanel implements TuiPanel
{
    /** High-level phase indicator displayed in the header. */
    public enum Phase
    {
        IDLE,
        DATA_GEN,
        COMPACTING,
        VALIDATING,
        COMPLETE,
        FAILED
    }

    private static final String TITLE = "Cassandra Compaction Validator";

    private int runNumber = 0;
    private long seed = 0L;
    private Phase phase = Phase.IDLE;
    private long runStartMs = 0L;
    private boolean runActive = false;

    @Override
    public void onEvent(TuiEvent event)
    {
        if (event instanceof TuiEvent.RunStart e)
        {
            this.runNumber = e.runNumber;
            this.seed = e.seed;
            this.phase = Phase.DATA_GEN;
            this.runStartMs = e.timestampMs;
            this.runActive = true;
        }
        else if (event instanceof TuiEvent.DataGenComplete)
        {
            this.phase = Phase.COMPACTING;
        }
        else if (event instanceof TuiEvent.CompactionComplete)
        {
            // Stay in COMPACTING until both backends finish.  When validation
            // begins we'll switch on ValidationProgress.
        }
        else if (event instanceof TuiEvent.ValidationProgress)
        {
            if (this.phase == Phase.COMPACTING || this.phase == Phase.DATA_GEN)
                this.phase = Phase.VALIDATING;
        }
        else if (event instanceof TuiEvent.ValidationComplete vc)
        {
            this.phase = vc.success ? Phase.COMPLETE : Phase.FAILED;
        }
        else if (event instanceof TuiEvent.RunComplete)
        {
            this.phase = Phase.COMPLETE;
            this.runActive = false;
        }
        else if (event instanceof TuiEvent.RunFailed)
        {
            this.phase = Phase.FAILED;
            this.runActive = false;
        }
    }

    @Override
    public void render(TextGraphics g, TerminalPosition origin, TerminalSize size)
    {
        if (size.getRows() < 1 || size.getColumns() < 10)
            return;

        // Wipe row first
        g.setBackgroundColor(TextColor.ANSI.DEFAULT);
        g.setForegroundColor(TextColor.ANSI.DEFAULT);
        g.fillRectangle(origin, new TerminalSize(size.getColumns(), 1), ' ');

        int row = origin.getRow();
        int col = origin.getColumn();
        int width = size.getColumns();

        // Title (cyan, bold)
        g.setForegroundColor(TextColor.ANSI.CYAN);
        g.putString(col, row, truncate(TITLE, width), SGR.BOLD);
        col += Math.min(TITLE.length(), width);
        if (col >= origin.getColumn() + width)
            return;

        // Separator
        g.setForegroundColor(TextColor.ANSI.WHITE);
        col = drawSeparator(g, row, col, origin.getColumn() + width);

        // Run number
        if (col >= origin.getColumn() + width)
            return;
        g.setForegroundColor(TextColor.ANSI.WHITE);
        String runStr = "Run #" + runNumber;
        g.putString(col, row, truncate(runStr, origin.getColumn() + width - col), SGR.BOLD);
        col += Math.min(runStr.length(), origin.getColumn() + width - col);

        // Separator
        col = drawSeparator(g, row, col, origin.getColumn() + width);

        // Seed (yellow)
        if (col < origin.getColumn() + width)
        {
            g.setForegroundColor(TextColor.ANSI.YELLOW);
            String seedStr = "seed=" + SeedUtil.toHex(seed);
            g.putString(col, row, truncate(seedStr, origin.getColumn() + width - col));
            col += Math.min(seedStr.length(), origin.getColumn() + width - col);
        }

        // Separator
        col = drawSeparator(g, row, col, origin.getColumn() + width);

        // Phase (green or red)
        if (col < origin.getColumn() + width)
        {
            TextColor phaseColor = phase == Phase.FAILED ? TextColor.ANSI.RED : TextColor.ANSI.GREEN;
            g.setForegroundColor(phaseColor);
            String phaseStr = "Phase: [" + phase.name() + "]";
            g.putString(col, row, truncate(phaseStr, origin.getColumn() + width - col), SGR.BOLD);
            col += Math.min(phaseStr.length(), origin.getColumn() + width - col);
        }

        // Separator
        col = drawSeparator(g, row, col, origin.getColumn() + width);

        // Elapsed
        if (col < origin.getColumn() + width)
        {
            g.setForegroundColor(TextColor.ANSI.WHITE);
            long elapsedMs = runActive && runStartMs > 0
                ? System.currentTimeMillis() - runStartMs
                : 0L;
            String elapsedStr = "Elapsed: " + formatHms(elapsedMs);
            g.putString(col, row, truncate(elapsedStr, origin.getColumn() + width - col));
        }
    }

    private int drawSeparator(TextGraphics g, int row, int col, int rightEdge)
    {
        if (col + 3 > rightEdge)
            return rightEdge;
        g.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);
        g.putString(col, row, " | ");
        return col + 3;
    }

    private static String formatHms(long ms)
    {
        long totalSec = ms / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private static String truncate(String s, int maxLen)
    {
        if (maxLen <= 0)
            return "";
        if (s.length() <= maxLen)
            return s;
        return s.substring(0, maxLen);
    }
}
