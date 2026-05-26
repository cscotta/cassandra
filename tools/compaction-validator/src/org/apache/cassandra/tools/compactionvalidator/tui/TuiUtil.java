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

import com.googlecode.lanterna.graphics.TextGraphics;

/**
 * Internal helpers shared by the TUI panels.  Not part of the public API.
 */
final class TuiUtil
{
    private TuiUtil()
    {
    }

    /** Truncates {@code s} to at most {@code maxLen} characters. */
    static String truncate(String s, int maxLen)
    {
        if (s == null)
            return "";
        if (maxLen <= 0)
            return "";
        if (s.length() <= maxLen)
            return s;
        if (maxLen <= 1)
            return s.substring(0, maxLen);
        return s.substring(0, maxLen - 1) + "…";
    }

    /** Right-pads {@code s} with spaces to {@code width} characters; truncates if too long. */
    static String pad(String s, int width)
    {
        if (s == null)
            s = "";
        if (s.length() == width)
            return s;
        if (s.length() > width)
            return truncate(s, width);
        StringBuilder sb = new StringBuilder(width);
        sb.append(s);
        for (int i = s.length(); i < width; i++)
            sb.append(' ');
        return sb.toString();
    }

    /**
     * Draws a horizontal section header in the form
     * {@code "─── Title ─────────────"} starting at {@code (col, row)} and
     * spanning {@code width} cells.  Uses the graphics' current foreground
     * colour.
     */
    static void drawSectionHeader(TextGraphics g, int col, int row, int width, String title)
    {
        if (width <= 0)
            return;
        StringBuilder sb = new StringBuilder(width);
        if (title == null || title.isEmpty())
        {
            for (int i = 0; i < width; i++)
                sb.append('─');
            g.putString(col, row, sb.toString());
            return;
        }
        sb.append("─── ").append(title).append(' ');
        while (sb.length() < width)
            sb.append('─');
        if (sb.length() > width)
            sb.setLength(width);
        g.putString(col, row, sb.toString());
    }

    /**
     * Renders a unicode progress bar of width {@code width} cells using
     * {@code █} for the filled portion and {@code ░} for the empty portion.
     */
    static String progressBar(double fraction, int width)
    {
        if (width <= 0)
            return "";
        if (fraction < 0)
            fraction = 0;
        if (fraction > 1)
            fraction = 1;
        int filled = (int) Math.round(fraction * width);
        if (filled > width)
            filled = width;
        StringBuilder sb = new StringBuilder(width);
        for (int i = 0; i < filled; i++)
            sb.append('█');
        for (int i = filled; i < width; i++)
            sb.append('░');
        return sb.toString();
    }

    /** Formats a non-negative byte count as e.g. {@code "1.2 GiB"}. */
    static String formatBytes(long bytes)
    {
        if (bytes < 0)
            bytes = 0;
        if (bytes >= 1024L * 1024L * 1024L)
            return String.format("%.1f GiB", bytes / (1024.0 * 1024.0 * 1024.0));
        if (bytes >= 1024L * 1024L)
            return String.format("%.1f MiB", bytes / (1024.0 * 1024.0));
        if (bytes >= 1024L)
            return String.format("%.1f KiB", bytes / 1024.0);
        return bytes + " B";
    }

    /** Formats a count with K/M/B suffixes for compactness. */
    static String formatCount(long count)
    {
        if (count >= 1_000_000_000L)
            return String.format("%.1fB", count / 1_000_000_000.0);
        if (count >= 1_000_000L)
            return String.format("%.1fM", count / 1_000_000.0);
        if (count >= 1_000L)
            return String.format("%.1fK", count / 1_000.0);
        return Long.toString(count);
    }

    /** Formats a duration in {@code mm:ss} or {@code hh:mm:ss}. */
    static String formatDuration(long ms)
    {
        if (ms < 0)
            ms = 0;
        long totalSec = ms / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        if (h > 0)
            return String.format("%02d:%02d:%02d", h, m, s);
        return String.format("%02d:%02d", m, s);
    }
}
