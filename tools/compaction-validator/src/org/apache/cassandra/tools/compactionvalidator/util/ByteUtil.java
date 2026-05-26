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
package org.apache.cassandra.tools.compactionvalidator.util;

/**
 * Utility methods for human-readable byte sizes and byte-count parsing.
 */
public final class ByteUtil
{
    private static final long KIB = 1024L;
    private static final long MIB = 1024L * KIB;
    private static final long GIB = 1024L * MIB;

    private ByteUtil()
    {
    }

    /**
     * Formats a byte count as a human-readable string using binary prefixes (KiB, MiB, GiB).
     *
     * <p>Examples:
     * <pre>
     *   humanReadable(1288490189)  → "1.2 GiB"
     *   humanReadable(536870912)   → "512.0 MiB"
     *   humanReadable(46387)       → "45.3 KiB"
     *   humanReadable(123)         → "123 B"
     * </pre>
     *
     * @param bytes non-negative byte count
     * @return formatted string
     */
    public static String humanReadable(long bytes)
    {
        if (bytes < 0)
            throw new IllegalArgumentException("byte count must be non-negative: " + bytes);

        if (bytes >= GIB)
            return String.format("%.1f GiB", (double) bytes / GIB);
        if (bytes >= MIB)
            return String.format("%.1f MiB", (double) bytes / MIB);
        if (bytes >= KIB)
            return String.format("%.1f KiB", (double) bytes / KIB);
        return bytes + " B";
    }

    /**
     * Parses a byte-count specification string into a {@code long} number of bytes.
     *
     * <p>Accepted suffixes (case-insensitive):
     * <ul>
     *   <li>{@code G}, {@code GiB}, {@code GB} — gibibytes (×1024³)</li>
     *   <li>{@code M}, {@code MiB}, {@code MB} — mebibytes (×1024²)</li>
     *   <li>{@code K}, {@code KiB}, {@code KB} — kibibytes (×1024)</li>
     *   <li>no suffix — plain bytes</li>
     * </ul>
     *
     * <p>Examples: {@code "10G"}, {@code "500M"}, {@code "1024K"}, {@code "1000"}
     *
     * @param spec the specification string (must not be null or blank)
     * @return the number of bytes represented by {@code spec}
     * @throws IllegalArgumentException if the string cannot be parsed
     */
    public static long parseBytes(String spec)
    {
        if (spec == null || spec.trim().isEmpty())
            throw new IllegalArgumentException("byte specification must not be null or blank");

        spec = spec.trim();
        String upper = spec.toUpperCase();

        if (upper.endsWith("GIB") || upper.endsWith("GB"))
        {
            return parseLong(spec, upper.endsWith("GIB") ? 3 : 2) * GIB;
        }
        if (upper.endsWith("MIB") || upper.endsWith("MB"))
        {
            return parseLong(spec, upper.endsWith("MIB") ? 3 : 2) * MIB;
        }
        if (upper.endsWith("KIB") || upper.endsWith("KB"))
        {
            return parseLong(spec, upper.endsWith("KIB") ? 3 : 2) * KIB;
        }
        if (upper.endsWith("G"))
            return parseLong(spec, 1) * GIB;
        if (upper.endsWith("M"))
            return parseLong(spec, 1) * MIB;
        if (upper.endsWith("K"))
            return parseLong(spec, 1) * KIB;

        // Plain numeric bytes
        try
        {
            return Long.parseLong(spec);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("Cannot parse byte specification: '" + spec + "'", e);
        }
    }

    private static long parseLong(String original, int suffixLength)
    {
        String numeric = original.substring(0, original.length() - suffixLength).trim();
        try
        {
            return Long.parseLong(numeric);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("Cannot parse byte specification: '" + original + "'", e);
        }
    }
}
