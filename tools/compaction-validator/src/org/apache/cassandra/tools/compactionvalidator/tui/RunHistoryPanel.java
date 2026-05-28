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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;

import org.apache.cassandra.tools.compactionvalidator.RunResult;
import org.apache.cassandra.tools.compactionvalidator.datagen.DataGenStats;

/**
 * Bottom-of-screen list of the most recent completed runs, providing quick
 * recall of what's been tried in the current session: each row shows when the
 * run started and finished, how long it took, the seed (so the user can repro),
 * the generated data size, and PASS/FAIL.
 *
 * <p>The panel takes the place of the (briefly redundant) "validation done"
 * state of {@link ValidationPanel} between runs: the validation bar still
 * tracks the current run only, and the history panel provides the persistent
 * record of past runs.
 */
public class RunHistoryPanel implements TuiPanel
{
    private static final String TITLE = "Recent Runs";

    /**
     * Cap on the number of run entries kept. The panel renders at most as many
     * rows as fit in its allocated height, so this bound is just a safety net
     * to keep memory predictable across long-running sessions.
     */
    private static final int MAX_HISTORY = 50;

    private static final DateTimeFormatter HMS = DateTimeFormatter.ofPattern("HH:mm:ss")
                                                                  .withZone(ZoneId.systemDefault());

    private final Deque<Entry> entries = new ArrayDeque<>();

    @Override
    public void onEvent(TuiEvent event)
    {
        if (event instanceof TuiEvent.RunComplete c)
            push(c.result);
        else if (event instanceof TuiEvent.RunFailed f)
            push(f.result);
    }

    private void push(RunResult r)
    {
        if (r == null)
            return;
        entries.addFirst(new Entry(r));
        while (entries.size() > MAX_HISTORY)
            entries.removeLast();
    }

    @Override
    public void render(TextGraphics g, TerminalPosition origin, TerminalSize size)
    {
        if (size.getRows() < 1 || size.getColumns() < 16)
            return;

        // Clear region (default bg)
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

        if (height < 2 || entries.isEmpty())
        {
            if (height >= 2 && entries.isEmpty())
            {
                g.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);
                g.putString(col, row + 1, TuiUtil.truncate("(no runs yet)", width));
            }
            return;
        }

        // Column-aligned rendering. Right-edge fits PASS/FAIL; left-edge holds run number;
        // middle has the readable bits. Sample, narrow:
        //   #00012  18:42:11→18:42:38  00:27   0xCAFEBABE12345678   341.2 MiB   PASS
        // We render as many rows as there are entries OR as fit, newest first.
        int maxRows = Math.min(height - 1, entries.size());
        int r = row + 1;
        int rendered = 0;
        for (Entry e : entries)
        {
            if (rendered >= maxRows)
                break;
            String line = formatLine(e, width);
            // Color code by result: green for PASS, red for FAIL. Default for the body.
            g.setForegroundColor(e.success ? TextColor.ANSI.WHITE : TextColor.ANSI.RED);
            if (e.success)
                g.putString(col, r, TuiUtil.truncate(line, width));
            else
                g.putString(col, r, TuiUtil.truncate(line, width), SGR.BOLD);
            r++;
            rendered++;
        }
    }

    private static String formatLine(Entry e, int width)
    {
        // Compact, right-trimmed-tolerant format. We assemble what we know up front and
        // let truncate() handle narrow terminals.
        String runStr  = String.format("#%-5d", e.runNumber);
        String window  = HMS.format(Instant.ofEpochMilli(e.startMs))
                       + "→" + HMS.format(Instant.ofEpochMilli(e.endMs));
        String dur     = TuiUtil.formatDuration(e.endMs - e.startMs);
        String seedStr = String.format("0x%016X", e.seed);
        String dataStr = TuiUtil.formatBytes(e.dataSizeBytes);
        String result  = e.success ? "PASS" : "FAIL";

        // " #N  start→end  dur   seed   size   RESULT"
        return String.format("%s  %s  %5s  %s  %9s  %s",
                             runStr, window, dur, seedStr, dataStr, result);
    }

    /** One row's worth of frozen state — captured at run-end so the panel doesn't keep RunResult refs alive. */
    private static final class Entry
    {
        final int runNumber;
        final long seed;
        final long startMs;
        final long endMs;
        final long dataSizeBytes;
        final boolean success;

        Entry(RunResult r)
        {
            this.runNumber = r.runNumber;
            this.seed = r.seed;
            this.startMs = r.startTimeMs;
            this.endMs = r.endTimeMs;
            DataGenStats dg = r.dataGenStats;
            this.dataSizeBytes = dg == null ? 0L : dg.getBytesWritten();
            this.success = r.success;
        }
    }
}
