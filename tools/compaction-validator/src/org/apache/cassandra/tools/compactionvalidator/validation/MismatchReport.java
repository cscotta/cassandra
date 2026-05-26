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
package org.apache.cassandra.tools.compactionvalidator.validation;

/**
 * Description of a single partition-level mismatch detected by {@link Validator}.
 *
 * <p>Instances are immutable.  All fields are public final to keep the data class
 * small and free of getter boilerplate; this class is intended to be displayed by
 * the TUI or written out to a log file by the caller.
 *
 * <p>The {@code legacyDump} and {@code cursorDump} fields are pretty-printed
 * representations of the offending partition on each side (suitable for diffing
 * by humans).  They may be very long; consumers wanting to truncate should do so
 * themselves.
 */
public final class MismatchReport
{
    /** Hex representation of the offending partition key. */
    public final String partitionKeyHex;

    /** Human-readable, single-sentence summary of <em>where</em> the two sides diverged. */
    public final String description;

    /** Pretty-printed dump of the partition contents from the legacy side. */
    public final String legacyDump;

    /** Pretty-printed dump of the partition contents from the cursor side. */
    public final String cursorDump;

    /**
     * Number of partitions that were successfully verified before this mismatch
     * was found.  Useful for sizing how deep into the compaction output the
     * divergence appeared.
     */
    public final long partitionsCheckedBeforeFailure;

    /**
     * @param partitionKeyHex                 hex representation of the offending partition key
     * @param description                     short summary of the divergence
     * @param legacyDump                      pretty-printed legacy-side partition contents
     * @param cursorDump                      pretty-printed cursor-side partition contents
     * @param partitionsCheckedBeforeFailure  number of partitions verified before this one
     */
    public MismatchReport(String partitionKeyHex,
                          String description,
                          String legacyDump,
                          String cursorDump,
                          long partitionsCheckedBeforeFailure)
    {
        this.partitionKeyHex = partitionKeyHex;
        this.description = description;
        this.legacyDump = legacyDump;
        this.cursorDump = cursorDump;
        this.partitionsCheckedBeforeFailure = partitionsCheckedBeforeFailure;
    }

    /**
     * Formats this report as a multi-line string suitable for display in a TUI panel
     * or for appending to a log file.
     *
     * <p>The format is stable: it begins with a single header line summarising the
     * mismatch, then prints the control-side dump under a {@code --- CONTROL ---}
     * banner, then the experiment-side dump under {@code --- EXPERIMENT ---}.
     * Consumers should not parse this format; it is for human consumption only.
     *
     * @return a multi-line string describing this mismatch
     */
    public String formatForDisplay()
    {
        StringBuilder sb = new StringBuilder(256 + safeLength(legacyDump) + safeLength(cursorDump));
        sb.append("MISMATCH at partition ").append(partitionKeyHex)
          .append(" (after ").append(partitionsCheckedBeforeFailure).append(" partitions checked)\n");
        sb.append("Description: ").append(description == null ? "(none)" : description).append('\n');
        sb.append('\n');
        sb.append("--- CONTROL ---\n");
        sb.append(legacyDump == null ? "(empty)" : legacyDump);
        if (legacyDump != null && !legacyDump.endsWith("\n"))
            sb.append('\n');
        sb.append("--- EXPERIMENT ---\n");
        sb.append(cursorDump == null ? "(empty)" : cursorDump);
        if (cursorDump != null && !cursorDump.endsWith("\n"))
            sb.append('\n');
        return sb.toString();
    }

    private static int safeLength(String s)
    {
        return s == null ? 0 : s.length();
    }

    @Override
    public String toString()
    {
        return "MismatchReport{partitionKey=" + partitionKeyHex
               + ", description='" + description + '\''
               + ", partitionsCheckedBeforeFailure=" + partitionsCheckedBeforeFailure
               + '}';
    }
}
