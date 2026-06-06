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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;

/**
 * Visualisation of a single backend's LSM tree.  Each emitted SSTable becomes
 * a "box" in the appropriate level row.  Recently-emitted boxes are rendered
 * in bright green for one second after the emit event, then fade to dim grey.
 *
 * <p>This widget is rendered <em>inside</em> a {@link CompactionPanel}; the
 * caller is responsible for the outer frame.  The widget itself fills the
 * given rectangle with rows of boxes, one row per visual level.
 */
public class LsmTreeWidget
{
    private static final long HIGHLIGHT_MILLIS = 1000L;

    /** A single SSTable rendered as a box in this widget. */
    private static final class Box
    {
        final String filename;
        final long sizeBytes;
        final int level;
        final long emittedAtMs;

        Box(String filename, long sizeBytes, int level, long emittedAtMs)
        {
            this.filename = filename;
            this.sizeBytes = sizeBytes;
            this.level = level;
            this.emittedAtMs = emittedAtMs;
        }
    }

    private final List<Box> boxes = new ArrayList<>();
    private boolean active = false;

    /** Adds a new SSTable box.  Marks the box as recently-emitted. */
    public void addSstable(String filename, long sizeBytes, int level)
    {
        boxes.add(new Box(filename, sizeBytes, level, System.currentTimeMillis()));
        active = true;
    }

    /** Marks the widget as inactive (no longer being written). */
    public void markInactive()
    {
        active = false;
    }

    /** Marks the widget as active (currently being written). */
    public void markActive()
    {
        active = true;
    }

    /** Clears all boxes from the widget. */
    public void clear()
    {
        boxes.clear();
        active = false;
    }

    /** @return whether this widget is currently active */
    public boolean isActive()
    {
        return active;
    }

    /**
     * Renders the widget's contents into the given rectangle.  The rectangle
     * is treated as the inner content area; the caller must have already
     * drawn any surrounding frame.
     */
    public void render(TextGraphics g, TerminalPosition origin, TerminalSize size)
    {
        if (size.getColumns() <= 0 || size.getRows() <= 0)
            return;

        // Clear region
        g.setBackgroundColor(TextColor.ANSI.DEFAULT);
        g.setForegroundColor(TextColor.ANSI.DEFAULT);
        g.fillRectangle(origin, size, ' ');

        // Group boxes by SIZE TIER, not by SSTable level. UCS doesn't use traditional levels —
        // every output sstable lands in level 0, so a level-grouped view is uninformative.
        // A size-tier view buckets by log2(size_in_MiB) so small flushes appear separately
        // from compacted larger sstables and the progression of compaction up the tiers is
        // visible. Tier 0 = <1 MiB, tier 1 = 1..2 MiB, tier 2 = 2..4 MiB, ..., tier 10 = 1..2 GiB.
        Map<Integer, List<Box>> byTier = new TreeMap<>(Comparator.reverseOrder());
        for (Box b : boxes)
            byTier.computeIfAbsent(sizeTier(b.sizeBytes), k -> new ArrayList<>()).add(b);

        if (byTier.isEmpty())
        {
            if (size.getRows() >= 1 && size.getColumns() >= 6)
            {
                g.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);
                g.putString(origin.getColumn(), origin.getRow(),
                            TuiUtil.truncate("(no SSTables yet)", size.getColumns()));
            }
            return;
        }

        long now = System.currentTimeMillis();
        int row = origin.getRow();
        int rowsAvailable = size.getRows();
        int width = size.getColumns();

        int rowIndex = 0;
        for (Map.Entry<Integer, List<Box>> entry : byTier.entrySet())
        {
            if (rowIndex >= rowsAvailable)
                break;

            int level = entry.getKey();
            List<Box> levelBoxes = entry.getValue();

            // Tier label like "T6  64M" — log2-bucketed by SSTable size with the bucket's
            // approximate lower bound shown so the operator can see at a glance which tier
            // is which. Right-padded so all tiers' boxes line up vertically.
            String prefix = String.format("%-7s", tierLabel(level));
            g.setForegroundColor(TextColor.ANSI.WHITE);
            g.putString(origin.getColumn(), row + rowIndex, prefix);

            int col = origin.getColumn() + prefix.length();
            int rightEdge = origin.getColumn() + width;

            for (Box b : levelBoxes)
            {
                String boxText = String.format("[■ %s] ", TuiUtil.formatBytes(b.sizeBytes));
                if (col + boxText.length() > rightEdge)
                {
                    // Out of room for this row — show ellipsis if we can
                    if (col + 3 <= rightEdge)
                    {
                        g.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);
                        g.putString(col, row + rowIndex, "…");
                    }
                    break;
                }
                long age = now - b.emittedAtMs;
                TextColor color;
                if (age < HIGHLIGHT_MILLIS)
                    color = TextColor.ANSI.GREEN_BRIGHT;
                else if (active)
                    color = TextColor.ANSI.YELLOW;
                else
                    color = TextColor.ANSI.BLACK_BRIGHT;
                g.setForegroundColor(color);
                if (age < HIGHLIGHT_MILLIS)
                    g.putString(col, row + rowIndex, boxText, SGR.BOLD);
                else
                    g.putString(col, row + rowIndex, boxText);
                col += boxText.length();
            }

            rowIndex++;
        }
    }

    /** @return number of SSTables tracked */
    public int sstableCount()
    {
        return boxes.size();
    }

    /**
     * Maps an SSTable size to a log2(MiB) tier index. Tier 0 covers <1 MiB, tier 1 covers
     * 1..2 MiB, tier 2 covers 2..4 MiB, ..., tier 10 covers 1..2 GiB. Sizes that don't
     * cleanly land in a single tier still bucket consistently.
     */
    static int sizeTier(long sizeBytes)
    {
        if (sizeBytes <= 0) return 0;
        long mib = Math.max(1L, sizeBytes / (1024L * 1024L));
        return 64 - Long.numberOfLeadingZeros(mib) - 1;
    }

    /** Human label for a tier — "T6  64M", "T10 1G". */
    static String tierLabel(int tier)
    {
        long lowerMib = 1L << Math.max(0, tier);
        // Compose like "T6 64M" / "T10 1G".
        String size = lowerMib >= 1024 ? (lowerMib / 1024) + "G" : lowerMib + "M";
        return "T" + tier + " " + size;
    }
}
