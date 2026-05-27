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
 * One byte-or-semantic well-formedness violation reported by {@link FormatAuditor}.
 *
 * <p>Each violation names the kind of invariant that was broken, the SSTable file
 * where it was found, the partition key (hex-encoded), and a free-form detail
 * string. Violations are immutable value objects suitable for inclusion in
 * {@code RunResult} and the run log.
 */
public final class FormatViolation
{
    /** The classes of invariant the auditor knows how to check. */
    public enum Kind
    {
        /**
         * A cell deserialised from the SSTable claims to be expiring (non-zero
         * TTL) but its {@code localDeletionTime} field doesn't satisfy the
         * expiration formula {@code (writetime / 1_000_000) + ttl}. The
         * IS_EXPIRING branch of the deserialiser reads {@code (ttl,
         * localDeletionTime)} as {@code (ttl, expiration_time)}; an
         * expiration_time that doesn't equal {@code writetime + ttl} can only
         * arise when the on-disk flags byte set both IS_DELETED and IS_EXPIRING
         * — the "{@code ldt}" value on disk was a deletion-time, not an
         * expiration-time. This is the signature of compaction bug 1A.
         */
        FLAG_DELETE_AND_EXPIRE("flag-delete-and-expire",
            "Cell carries IS_EXPIRING semantics but its localDeletionTime field does "
            + "not match writetime + ttl — indicates the on-disk flags byte had both "
            + "IS_DELETED and IS_EXPIRING bits set (cursor compaction bug 1A)."),

        /**
         * Two consecutive rows in the same partition have clusterings that don't
         * strictly increase in the partition's clustering order. Cassandra's
         * read path requires strictly-increasing clusterings within a partition;
         * a violation either fails the partition iterator outright or silently
         * returns the wrong row on indexed reads.
         *
         * <p>This kind covers the strict {@code prev > curr} case (true
         * reverse ordering) only. The separate {@link #DUPLICATE_CLUSTERING}
         * kind covers {@code prev == curr}, which is structurally invalid but
         * is the more common downstream symptom of the bug-1A flag-byte
         * cascade — a misaligned deserialiser reads bytes that happen to
         * parse as a "row marker" with the same clustering as the previous
         * one. Keeping the two cases distinct lets the errata matcher
         * suppress the cascade while still failing loudly on a real
         * out-of-order writer bug.
         */
        ROW_OUT_OF_ORDER("row-out-of-order",
            "Rows within a partition are not in strictly-increasing clustering order "
            + "(prev > curr). Indicates the writer emitted unfiltereds in the wrong "
            + "sequence."),

        /**
         * Two consecutive rows in the same partition share the same clustering
         * value ({@code prev == curr}). For a CK'd table this is a real format
         * violation; for a 0-CK table where every row's clustering is empty,
         * multiple rows per partition is structurally impossible (CQL merges
         * them into one).
         *
         * <p>In practice this kind almost always fires as a downstream symptom
         * of compaction bug 1A: the deserialiser drifts past a misaligned
         * flag-byte cell, reads garbage bytes that happen to parse as a row
         * marker with the same clustering as the one before, and reports the
         * "duplicate". Mapped to the same errata rule as the upstream bug.
         */
        DUPLICATE_CLUSTERING("duplicate-clustering",
            "Rows within a partition repeat the same clustering value. Typically "
            + "the downstream symptom of bug-1A's flag-byte cascade rather than a "
            + "real row-ordering bug; mapped to the same errata suppression."),

        /**
         * A cell's timestamp is negative, i.e. before the Unix epoch. Cassandra's
         * mutation paths reject negative timestamps; seeing one in a compacted
         * SSTable means a compaction bug fabricated a bogus timestamp.
         */
        TIMESTAMP_NEGATIVE("timestamp-negative",
            "Cell has a negative timestamp — invalid by spec; indicates the writer "
            + "fabricated a malformed cell."),

        /**
         * A cell's TTL is negative. CQL only allows TTLs in {@code [0, MaxTtl]};
         * a negative TTL on disk indicates corruption or a writer bug.
         */
        TTL_NEGATIVE("ttl-negative",
            "Cell has a negative TTL — invalid by spec; indicates the writer "
            + "fabricated a malformed cell."),

        /**
         * The scanner walking the SSTable threw before completing the audit —
         * typically {@code CorruptSSTableException} but any throwable from the
         * deserialiser ends up here. Historically the cascading downstream
         * symptom of compaction bug 1A: once the writer emits a cell whose
         * flag-byte has both {@code IS_DELETED} and {@code IS_EXPIRING} set
         * over an IS_DELETED-format payload, the reader's IS_EXPIRING branch
         * consumes the wrong byte for the TTL field, drifts further off
         * alignment on the next path/value read, and eventually trips
         * deserialiser bounds checks on a downstream cell.
         *
         * <p>Mapped by {@link ErrataRule#forFormatViolation} to the same
         * {@link ErrataRule#TOMBSTONE_EXPIRING_FLAGS_BOTH_SET} rule that
         * suppresses the upstream {@link #FLAG_DELETE_AND_EXPIRE} violations,
         * so soak runs with that rule active ride through the whole cascade
         * — a new exception class that isn't a known symptom would surface
         * here too, but at least the soak loop keeps running rather than
         * halting on every iteration that lands on the bug.
         */
        SCANNER_FAILURE("scanner-failure",
            "Scanner threw while walking an SSTable — typically the cascading "
            + "symptom of compaction bug 1A's flag-byte corruption that leaves "
            + "downstream cells unreadable.");

        private final String cliName;
        private final String description;

        Kind(String cliName, String description)
        {
            this.cliName = cliName;
            this.description = description;
        }

        /** @return the lower-case dash-separated name used in run logs / errata rules */
        public String cliName() { return cliName; }
        /** @return one-line description of what the invariant says */
        public String description() { return description; }
    }

    public final Kind kind;
    public final String sstableFilename;
    public final String partitionKeyHex;
    public final String detail;

    public FormatViolation(Kind kind, String sstableFilename, String partitionKeyHex, String detail)
    {
        if (kind == null)
            throw new IllegalArgumentException("kind must not be null");
        this.kind = kind;
        this.sstableFilename = sstableFilename != null ? sstableFilename : "(unknown)";
        this.partitionKeyHex = partitionKeyHex != null ? partitionKeyHex : "(unknown)";
        this.detail = detail != null ? detail : "";
    }

    @Override
    public String toString()
    {
        return "FormatViolation{kind=" + kind.cliName()
               + ", sstable=" + sstableFilename
               + ", partition=" + partitionKeyHex
               + ", detail=" + detail
               + '}';
    }
}
