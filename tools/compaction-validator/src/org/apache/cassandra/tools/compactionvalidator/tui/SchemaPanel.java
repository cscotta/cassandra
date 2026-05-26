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
import java.util.Collections;
import java.util.List;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;

/**
 * Multi-line panel showing the {@code CREATE TABLE} CQL string for the
 * current run.  If the schema is taller than the available rows, the panel
 * shows a leading slice and an ellipsis on the final line.
 */
public class SchemaPanel implements TuiPanel
{
    private static final String TITLE = "Schema";

    private List<String> cqlLines = Collections.emptyList();

    /**
     * Returns the number of rows this panel would like, given the schema currently
     * loaded. Includes 1 row for the section header. Returns 0 when no schema is
     * loaded yet, so the layout can collapse the panel until the first run starts.
     */
    public int getDesiredHeight()
    {
        if (cqlLines.isEmpty())
            return 0;
        return cqlLines.size() + 1; // +1 for the section header line
    }

    @Override
    public void onEvent(TuiEvent event)
    {
        if (event instanceof TuiEvent.SchemaReady e && e.schema != null)
        {
            String cql = e.schema.getCql();
            this.cqlLines = cql == null ? Collections.emptyList() : splitLines(cql);
        }
        else if (event instanceof TuiEvent.RunStart)
        {
            // Clear stale schema from any previous run
            this.cqlLines = Collections.emptyList();
        }
    }

    @Override
    public void render(TextGraphics g, TerminalPosition origin, TerminalSize size)
    {
        if (size.getRows() < 1 || size.getColumns() < 4)
            return;

        // Clear region
        g.setBackgroundColor(TextColor.ANSI.DEFAULT);
        g.setForegroundColor(TextColor.ANSI.DEFAULT);
        g.fillRectangle(origin, size, ' ');

        int row = origin.getRow();
        int col = origin.getColumn();
        int width = size.getColumns();
        int height = size.getRows();

        // Frame title
        g.setForegroundColor(TextColor.ANSI.WHITE);
        TuiUtil.drawSectionHeader(g, col, row, width, TITLE);

        if (height < 2)
            return;

        // Body: render lines below the header
        int bodyRow = row + 1;
        int bodyHeight = height - 1;
        int totalLines = cqlLines.size();
        if (totalLines == 0)
        {
            g.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);
            g.putString(col + 1, bodyRow, TuiUtil.truncate("(awaiting schema)", width - 2));
            return;
        }

        boolean truncated = totalLines > bodyHeight;
        int linesToShow = truncated ? bodyHeight - 1 : totalLines;

        g.setForegroundColor(TextColor.ANSI.GREEN);
        for (int i = 0; i < linesToShow; i++)
        {
            String line = cqlLines.get(i);
            g.putString(col + 1, bodyRow + i, TuiUtil.truncate(line, width - 2));
        }

        if (truncated)
        {
            g.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);
            int dotsRow = bodyRow + bodyHeight - 1;
            String suffix = String.format("... (%d more lines)", totalLines - linesToShow);
            g.putString(col + 1, dotsRow, TuiUtil.truncate(suffix, width - 2), SGR.ITALIC);
        }
    }

    private static List<String> splitLines(String s)
    {
        List<String> out = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < s.length(); i++)
        {
            if (s.charAt(i) == '\n')
            {
                out.add(s.substring(start, i));
                start = i + 1;
            }
        }
        if (start < s.length())
            out.add(s.substring(start));
        return out;
    }
}
