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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.BufferDecoratedKey;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.RegularAndStaticColumns;
import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.marshal.Int32Type;
import org.apache.cassandra.db.rows.AbstractUnfilteredRowIterator;
import org.apache.cassandra.db.rows.BTreeRow;
import org.apache.cassandra.db.rows.BufferCell;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.EncodingStats;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.Rows;
import org.apache.cassandra.db.rows.Unfiltered;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.dht.Murmur3Partitioner;
import org.apache.cassandra.schema.ColumnMetadata;
import org.apache.cassandra.schema.TableMetadata;
import org.apache.cassandra.utils.ByteBufferUtil;

/**
 * Unit tests for {@link ErrataChecker} — synthesises partition pairs that match
 * (or specifically don't match) the {@code equal-ts-tiebreaker} cursor-compaction
 * defect and verifies the matcher's classification:
 *
 * <ul>
 *   <li>identical partitions → empty rule set (no errata fired)</li>
 *   <li>cell-value-only diff at equal timestamp → rule fires</li>
 *   <li>cell-value diff + timestamp diff → empty set (real mismatch)</li>
 *   <li>missing row on one side → empty set (real mismatch)</li>
 *   <li>no rules active → empty set (matcher is a no-op)</li>
 *   <li>static row covers the exact pattern from the original bug report</li>
 * </ul>
 *
 * <p>This is the cheap proof-of-correctness for the matcher; the end-to-end
 * "real seed reproduces" verification is in the soak-run integration tests.
 */
public class ErrataCheckerTest
{
    private static final TableMetadata METADATA;
    private static final ColumnMetadata STATIC_BLOB;
    private static final ColumnMetadata REGULAR_INT;

    static
    {
        METADATA = TableMetadata.builder("ks", "cf")
                                .addPartitionKeyColumn("pk", Int32Type.instance)
                                .addClusteringColumn("ck", Int32Type.instance)
                                .addStaticColumn("s1", BytesType.instance)
                                .addRegularColumn("v", Int32Type.instance)
                                .partitioner(Murmur3Partitioner.instance)
                                .offline()
                                .build();
        STATIC_BLOB = METADATA.regularAndStaticColumns().columns(true).getSimple(0);
        REGULAR_INT = METADATA.regularAndStaticColumns().columns(false).getSimple(0);
    }

    @BeforeClass
    public static void setUp()
    {
        DatabaseDescriptor.clientInitialization(false);
    }

    @Test
    public void noActiveRulesIsNoOp()
    {
        ErrataChecker c = new ErrataChecker(Collections.emptySet());
        Assert.assertFalse(c.isActive());

        // Two divergent partitions — without active rules, matcher should report
        // empty set (caller treats as real mismatch).
        Set<ErrataRule> fired = c.matches(
            makePartition(1, makeRowWithCell(1, intCell(REGULAR_INT, 10, 1L))),
            makePartition(1, makeRowWithCell(1, intCell(REGULAR_INT, 20, 1L))));
        Assert.assertTrue("matcher must be a no-op when no rules active", fired.isEmpty());
    }

    @Test
    public void identicalPartitionsFireNoRules()
    {
        ErrataChecker c = new ErrataChecker(EnumSet.allOf(ErrataRule.class));
        Set<ErrataRule> fired = c.matches(
            makePartition(1, makeRowWithCell(1, intCell(REGULAR_INT, 10, 1L))),
            makePartition(1, makeRowWithCell(1, intCell(REGULAR_INT, 10, 1L))));
        Assert.assertTrue("identical partitions should fire no errata rules", fired.isEmpty());
    }

    @Test
    public void equalTimestampValueDiffFiresRule()
    {
        // The bug signature: two cells in the same row, same column, same
        // timestamp, same TTL, same localDeletionTime — but DIFFERENT value bytes.
        // Legacy correctly keeps the larger value (0x20); cursor incorrectly keeps
        // the smaller (0x10). ErrataChecker should classify this as the known rule.
        ErrataChecker c = new ErrataChecker(EnumSet.of(ErrataRule.EQUAL_TIMESTAMP_TIEBREAKER));
        Set<ErrataRule> fired = c.matches(
            makePartition(1, makeRowWithCell(1, intCell(REGULAR_INT, 0x20, 1779670427680000L))),
            makePartition(1, makeRowWithCell(1, intCell(REGULAR_INT, 0x10, 1779670427680000L))));

        Assert.assertEquals("equal-ts value-only diff must fire EQUAL_TIMESTAMP_TIEBREAKER",
                            EnumSet.of(ErrataRule.EQUAL_TIMESTAMP_TIEBREAKER), fired);
    }

    @Test
    public void valueDiffWithDifferentTimestampIsRealMismatch()
    {
        // Value differs AND timestamps differ — that's not the tie-breaker bug,
        // it's a real timestamp-resolution divergence. Rule must NOT fire.
        ErrataChecker c = new ErrataChecker(EnumSet.of(ErrataRule.EQUAL_TIMESTAMP_TIEBREAKER));
        Set<ErrataRule> fired = c.matches(
            makePartition(1, makeRowWithCell(1, intCell(REGULAR_INT, 0x20, 1L))),
            makePartition(1, makeRowWithCell(1, intCell(REGULAR_INT, 0x10, 2L))));

        Assert.assertTrue("different-timestamp diff must NOT be suppressed by equal-ts rule",
                          fired.isEmpty());
    }

    @Test
    public void staticRowEqualTimestampValueDiffFiresRule()
    {
        // The original reported bug: STATIC blob cell with equal timestamps and
        // differing value bytes. Same path through ErrataChecker as regular cells,
        // but exercises the staticRow branch explicitly.
        ByteBuffer larger  = ByteBuffer.wrap(new byte[] { (byte) 0xb9, (byte) 0xe5 });
        ByteBuffer smaller = ByteBuffer.wrap(new byte[] { (byte) 0x94, (byte) 0xf2 });
        long ts = 1779670427680000L;

        ErrataChecker c = new ErrataChecker(EnumSet.of(ErrataRule.EQUAL_TIMESTAMP_TIEBREAKER));
        Set<ErrataRule> fired = c.matches(
            makePartitionWithStatic(1, larger,  ts),
            makePartitionWithStatic(1, smaller, ts));

        Assert.assertEquals("static-cell equal-ts diff must fire EQUAL_TIMESTAMP_TIEBREAKER",
                            EnumSet.of(ErrataRule.EQUAL_TIMESTAMP_TIEBREAKER), fired);
    }

    @Test
    public void missingRowIsRealMismatch()
    {
        // Legacy has two rows, cursor has one — no rule covers a missing-row
        // divergence. Must surface as real mismatch.
        ErrataChecker c = new ErrataChecker(EnumSet.of(ErrataRule.EQUAL_TIMESTAMP_TIEBREAKER));
        Set<ErrataRule> fired = c.matches(
            makePartition(1, makeRowWithCell(1, intCell(REGULAR_INT, 10, 1L)),
                            makeRowWithCell(2, intCell(REGULAR_INT, 20, 1L))),
            makePartition(1, makeRowWithCell(1, intCell(REGULAR_INT, 10, 1L))));

        Assert.assertTrue("missing row must NOT be suppressed", fired.isEmpty());
    }

    @Test
    public void cliParseListRejectsUnknownRules()
    {
        // The CLI parser should fail loudly on typos rather than silently no-op'ing.
        try
        {
            ErrataRule.parseCliList("equal-ts-tiebreaker,bogus-rule");
            Assert.fail("expected IllegalArgumentException on unknown rule");
        }
        catch (IllegalArgumentException expected)
        {
            Assert.assertTrue("error message should name the unknown rule",
                              expected.getMessage().contains("bogus-rule"));
        }
    }

    @Test
    public void cliParseListAcceptsEmptyAndNull()
    {
        Assert.assertTrue(ErrataRule.parseCliList(null).isEmpty());
        Assert.assertTrue(ErrataRule.parseCliList("").isEmpty());
        Assert.assertTrue(ErrataRule.parseCliList(" , , ").isEmpty());
    }

    @Test
    public void tombstoneVsExpiringSameTimestampFiresRule1B()
    {
        // Bug 1B signature: legacy emits a tombstone (zero TTL, non-empty
        // localDeletionTime) and cursor emits an expiring cell (non-zero TTL) at
        // the SAME timestamp. With the old matcher this would have been classified
        // as a real mismatch (multi-field difference); the
        // TOMBSTONE_RESURRECTED_BY_EXPIRING rule's broad matcher should suppress it.
        long sharedTimestamp = 1779670427680000L;
        Cell<?> legacyTombstone = new BufferCell(REGULAR_INT,
                                                  sharedTimestamp,
                                                  Cell.NO_TTL,
                                                  /* localDeletionTime = */ 1779670427,
                                                  ByteBufferUtil.EMPTY_BYTE_BUFFER,
                                                  null);
        Cell<?> cursorExpiring = new BufferCell(REGULAR_INT,
                                                 sharedTimestamp,
                                                 /* ttl = */ 86400,
                                                 /* localExpirationTime = */ 1779756827,
                                                 ByteBufferUtil.bytes(42),
                                                 null);

        ErrataChecker c = new ErrataChecker(EnumSet.of(ErrataRule.TOMBSTONE_RESURRECTED_BY_EXPIRING));
        Set<ErrataRule> fired = c.matches(
            makePartition(1, makeRowWithCell(1, legacyTombstone)),
            makePartition(1, makeRowWithCell(1, cursorExpiring)));

        Assert.assertEquals("tombstone-vs-expiring at same TS must fire TOMBSTONE_RESURRECTED_BY_EXPIRING",
                            EnumSet.of(ErrataRule.TOMBSTONE_RESURRECTED_BY_EXPIRING), fired);
    }

    @Test
    public void tombstoneVsExpiringDifferentTimestampIsRealMismatch()
    {
        // Same shape as the previous test but with mismatched timestamps. The bug-1B
        // signature requires equal timestamps; without that, the difference is some
        // OTHER kind of bug and the rule must NOT suppress it.
        Cell<?> legacyTombstone = new BufferCell(REGULAR_INT,
                                                  1L,
                                                  Cell.NO_TTL,
                                                  100,
                                                  ByteBufferUtil.EMPTY_BYTE_BUFFER,
                                                  null);
        Cell<?> cursorExpiring = new BufferCell(REGULAR_INT,
                                                 2L,    // ← different timestamp
                                                 86400,
                                                 86500,
                                                 ByteBufferUtil.bytes(42),
                                                 null);

        ErrataChecker c = new ErrataChecker(EnumSet.of(ErrataRule.TOMBSTONE_RESURRECTED_BY_EXPIRING));
        Set<ErrataRule> fired = c.matches(
            makePartition(1, makeRowWithCell(1, legacyTombstone)),
            makePartition(1, makeRowWithCell(1, cursorExpiring)));

        Assert.assertTrue("different-TS tombstone-vs-expiring must NOT be suppressed", fired.isEmpty());
    }

    @Test
    public void brokenExpirationFormulaFiresRule1A()
    {
        // Bug 1A signature: legacy emits a properly-formed expiring cell where
        // localDeletionTime == writetime + ttl. Cursor's IS_DELETED+IS_EXPIRING
        // double-flag ends up reading a deletion-time as the expiration field, so
        // localDeletionTime != writetime + ttl. The
        // TOMBSTONE_EXPIRING_FLAGS_BOTH_SET rule recognises this signature on the
        // cursor side.
        long timestamp = 1_700_000_000_000_000L;  // micros
        long writetimeSec = timestamp / 1_000_000L;
        int ttl = 86_400;
        // Legacy: properly formed expiring cell (ldt = writetime + ttl)
        Cell<?> legacyExpiring = new BufferCell(REGULAR_INT,
                                                 timestamp,
                                                 ttl,
                                                 (int) (writetimeSec + ttl),
                                                 ByteBufferUtil.bytes(42),
                                                 null);
        // Cursor: ldt is some unrelated deletion-time, NOT writetime+ttl
        int bogusLdt = (int) (writetimeSec + ttl + 100_000);  // way off
        Cell<?> cursorBroken = new BufferCell(REGULAR_INT,
                                               timestamp,
                                               ttl,
                                               bogusLdt,
                                               ByteBufferUtil.bytes(42),
                                               null);

        ErrataChecker c = new ErrataChecker(EnumSet.of(ErrataRule.TOMBSTONE_EXPIRING_FLAGS_BOTH_SET));
        Set<ErrataRule> fired = c.matches(
            makePartition(1, makeRowWithCell(1, legacyExpiring)),
            makePartition(1, makeRowWithCell(1, cursorBroken)));

        Assert.assertEquals("broken expiration-formula on cursor must fire TOMBSTONE_EXPIRING_FLAGS_BOTH_SET",
                            EnumSet.of(ErrataRule.TOMBSTONE_EXPIRING_FLAGS_BOTH_SET), fired);
    }

    @Test
    public void allFiveNewRulesCanBeListedOnCli()
    {
        // Sanity that the CLI parser accepts every new rule name we registered.
        // Pre-existing rule plus the five additions = six rules total.
        Set<ErrataRule> rules = ErrataRule.parseCliList(
            "equal-ts-tiebreaker,"
            + "tombstone-expiring-flags-both-set,"
            + "tombstone-resurrected-by-expiring,"
            + "prev-unfiltered-size-zero,"
            + "index-block-width-off-by-one,"
            + "wide-column-subset-dropped-last");
        Assert.assertEquals("CLI parse must yield all six rule values",
                            EnumSet.allOf(ErrataRule.class), rules);
    }

    @Test
    public void formatViolationKindMapsToCorrectRule()
    {
        // Bug-1A signatures — the cell-level FLAG_DELETE_AND_EXPIRE plus the
        // four downstream cascade symptoms (DUPLICATE_CLUSTERING from a row
        // marker re-parse, TIMESTAMP_NEGATIVE / TTL_NEGATIVE from misaligned
        // varint reads, SCANNER_FAILURE from the eventual bounds-check) all
        // map to the same suppression rule so a soak run with
        // TOMBSTONE_EXPIRING_FLAGS_BOTH_SET active rides through the entire
        // cascade.
        Assert.assertEquals(ErrataRule.TOMBSTONE_EXPIRING_FLAGS_BOTH_SET,
                            ErrataRule.forFormatViolation(FormatViolation.Kind.FLAG_DELETE_AND_EXPIRE));
        Assert.assertEquals(ErrataRule.TOMBSTONE_EXPIRING_FLAGS_BOTH_SET,
                            ErrataRule.forFormatViolation(FormatViolation.Kind.DUPLICATE_CLUSTERING));
        Assert.assertEquals(ErrataRule.TOMBSTONE_EXPIRING_FLAGS_BOTH_SET,
                            ErrataRule.forFormatViolation(FormatViolation.Kind.TIMESTAMP_NEGATIVE));
        Assert.assertEquals(ErrataRule.TOMBSTONE_EXPIRING_FLAGS_BOTH_SET,
                            ErrataRule.forFormatViolation(FormatViolation.Kind.TTL_NEGATIVE));
        Assert.assertEquals(ErrataRule.TOMBSTONE_EXPIRING_FLAGS_BOTH_SET,
                            ErrataRule.forFormatViolation(FormatViolation.Kind.SCANNER_FAILURE));

        // Only strict-reverse row ordering stays unmapped — would indicate a
        // real writer bug outside the known bug-1A cascade.
        Assert.assertNull(ErrataRule.forFormatViolation(FormatViolation.Kind.ROW_OUT_OF_ORDER));

        // Defensive: null in → null out.
        Assert.assertNull(ErrataRule.forFormatViolation(null));
    }

    @Test
    public void emptyVsExpiringRowFiresRule1B()
    {
        // Bug-1B static-row signature: legacy's same-TS tombstones collapse the
        // static row to empty via Row.Merger; cursor's bug-1B keeps an expiring
        // cell that survives. The errata matcher needs to recognise this as
        // bug-1B even though there's no cell-pair to compare (legacy side has
        // no cells at all).
        long sharedTimestamp = 1779861176443000L;
        Cell<?> cursorExpiring = new BufferCell(REGULAR_INT,
                                                 sharedTimestamp,
                                                 /* ttl = */ 873289,
                                                 /* localExpirationTime = */ 1780734465,
                                                 ByteBufferUtil.bytes(42),
                                                 null);

        ErrataChecker c = new ErrataChecker(EnumSet.of(ErrataRule.TOMBSTONE_RESURRECTED_BY_EXPIRING));
        // Cursor side has a body row with the expiring cell; legacy has no row
        // at the same clustering — the row-asymmetry direction the new matcher
        // covers.
        Set<ErrataRule> fired = c.matches(
            makePartition(1),
            makePartition(1, makeRowWithCell(1, cursorExpiring)));

        Assert.assertEquals("empty-vs-expiring row asymmetry must fire TOMBSTONE_RESURRECTED_BY_EXPIRING",
                            EnumSet.of(ErrataRule.TOMBSTONE_RESURRECTED_BY_EXPIRING), fired);
    }

    @Test
    public void emptyVsPureLiveRowIsRealMismatch()
    {
        // Same shape as the previous test but cursor's cell is a pure live cell
        // (no TTL). That signature isn't bug-1B — under bug-1B, the surviving
        // cell carries the TTL of the expiring INSERT that beat the tombstone.
        // The matcher must NOT suppress this.
        Cell<?> cursorLive = new BufferCell(REGULAR_INT,
                                             1779861176443000L,
                                             Cell.NO_TTL,
                                             Cell.NO_DELETION_TIME,
                                             ByteBufferUtil.bytes(42),
                                             null);

        ErrataChecker c = new ErrataChecker(EnumSet.of(ErrataRule.TOMBSTONE_RESURRECTED_BY_EXPIRING));
        Set<ErrataRule> fired = c.matches(
            makePartition(1),
            makePartition(1, makeRowWithCell(1, cursorLive)));

        Assert.assertTrue("pure-live row with no legacy counterpart must NOT be suppressed",
                          fired.isEmpty());
    }

    @Test
    public void cursorEmptyLegacyNonEmptyIsRealMismatch()
    {
        // The OPPOSITE asymmetry direction — cursor lost data the legacy side
        // kept — has no known bug-1B variant, so the matcher must NOT suppress
        // it even when the rule is active.
        Cell<?> legacyLive = new BufferCell(REGULAR_INT,
                                             1779861176443000L,
                                             Cell.NO_TTL,
                                             Cell.NO_DELETION_TIME,
                                             ByteBufferUtil.bytes(42),
                                             null);

        ErrataChecker c = new ErrataChecker(EnumSet.of(ErrataRule.TOMBSTONE_RESURRECTED_BY_EXPIRING));
        Set<ErrataRule> fired = c.matches(
            makePartition(1, makeRowWithCell(1, legacyLive)),
            makePartition(1));

        Assert.assertTrue("cursor-lost-data direction must NOT be suppressed",
                          fired.isEmpty());
    }

    @Test
    public void cursorExtraExpiringColumnFiresRule1B()
    {
        // Column-within-row manifestation of bug 1B: both sides have the row,
        // but cursor's row has an extra expiring cell that legacy's doesn't.
        // Legacy's body row contains REGULAR_INT only (live, value=7);
        // cursor's body row contains the same REGULAR_INT (live, value=7) plus
        // an extra STATIC_BLOB cell that's expiring (the bug-1B-resurrected
        // cell from a same-TS write where legacy correctly tombstoned). The
        // merge-walk skips cursor's extra STATIC_BLOB via the per-cell matcher.
        long sharedTimestamp = 1779863222687000L;
        Cell<?> sharedRegular = intCell(REGULAR_INT, 7, sharedTimestamp);
        Cell<?> cursorRegularCopy = intCell(REGULAR_INT, 7, sharedTimestamp);
        Cell<?> cursorExtraExpiring = new BufferCell(STATIC_BLOB,
                                                      sharedTimestamp,
                                                      /* ttl = */ 873289,
                                                      /* localExpirationTime = */ 1780734465,
                                                      ByteBuffer.wrap(new byte[]{ 0x10, 0x20 }),
                                                      null);

        Row legacyRow = makeRowWithCells(1, sharedRegular);
        Row cursorRow = makeRowWithCells(1, cursorRegularCopy, cursorExtraExpiring);

        ErrataChecker c = new ErrataChecker(EnumSet.of(ErrataRule.TOMBSTONE_RESURRECTED_BY_EXPIRING));
        Set<ErrataRule> fired = c.matches(
            makePartition(1, legacyRow),
            makePartition(1, cursorRow));

        Assert.assertEquals("cursor-extra-expiring-column asymmetry must fire TOMBSTONE_RESURRECTED_BY_EXPIRING",
                            EnumSet.of(ErrataRule.TOMBSTONE_RESURRECTED_BY_EXPIRING), fired);
    }

    @Test
    public void cursorExtraPureLiveColumnIsRealMismatch()
    {
        // Same shape as the previous test but cursor's extra cell is pure-live
        // (no TTL). That's not a bug-1B signature — under bug-1B the surviving
        // cell carries the TTL of the expiring INSERT. Must NOT be suppressed.
        long sharedTimestamp = 1779863222687000L;
        Cell<?> sharedRegular = intCell(REGULAR_INT, 7, sharedTimestamp);
        Cell<?> cursorRegularCopy = intCell(REGULAR_INT, 7, sharedTimestamp);
        Cell<?> cursorExtraLive = new BufferCell(STATIC_BLOB,
                                                  sharedTimestamp,
                                                  Cell.NO_TTL,
                                                  Cell.NO_DELETION_TIME,
                                                  ByteBuffer.wrap(new byte[]{ 0x10, 0x20 }),
                                                  null);

        Row legacyRow = makeRowWithCells(1, sharedRegular);
        Row cursorRow = makeRowWithCells(1, cursorRegularCopy, cursorExtraLive);

        ErrataChecker c = new ErrataChecker(EnumSet.of(ErrataRule.TOMBSTONE_RESURRECTED_BY_EXPIRING));
        Set<ErrataRule> fired = c.matches(
            makePartition(1, legacyRow),
            makePartition(1, cursorRow));

        Assert.assertTrue("cursor-extra-pure-live column must NOT be suppressed", fired.isEmpty());
    }

    // ---- Helpers --------------------------------------------------------

    private static DecoratedKey dk(int pk)
    {
        ByteBuffer key = ByteBufferUtil.bytes(pk);
        return new BufferDecoratedKey(METADATA.partitioner.getToken(key), key);
    }

    private static UnfilteredRowIterator makePartition(int pk, Row... rows)
    {
        final Iterator<Row> it = Arrays.asList(rows).iterator();
        return new AbstractUnfilteredRowIterator(METADATA,
                                                 dk(pk),
                                                 DeletionTime.LIVE,
                                                 METADATA.regularAndStaticColumns(),
                                                 Rows.EMPTY_STATIC_ROW,
                                                 false,
                                                 EncodingStats.NO_STATS)
        {
            @Override
            protected Unfiltered computeNext()
            {
                return it.hasNext() ? it.next() : endOfData();
            }
        };
    }

    /**
     * Partition with a single static-cell value (no body rows). Mirrors the
     * shape of the bug-report partition: only the static row differs.
     */
    private static UnfilteredRowIterator makePartitionWithStatic(int pk, ByteBuffer staticBlobValue, long ts)
    {
        Row.Builder b = BTreeRow.sortedBuilder();
        b.newRow(org.apache.cassandra.db.Clustering.STATIC_CLUSTERING);
        b.addCell(new BufferCell(STATIC_BLOB, ts, Cell.NO_TTL, Cell.NO_DELETION_TIME, staticBlobValue, null));
        final Row staticRow = b.build();

        return new AbstractUnfilteredRowIterator(METADATA,
                                                 dk(pk),
                                                 DeletionTime.LIVE,
                                                 METADATA.regularAndStaticColumns(),
                                                 staticRow,
                                                 false,
                                                 EncodingStats.NO_STATS)
        {
            @Override
            protected Unfiltered computeNext()
            {
                return endOfData();
            }
        };
    }

    private static Row makeRowWithCell(int ck, Cell<?> c)
    {
        Row.Builder b = BTreeRow.sortedBuilder();
        b.newRow(METADATA.comparator.make(ck));
        b.addCell(c);
        return b.build();
    }

    private static Row makeRowWithCells(int ck, Cell<?>... cells)
    {
        Row.Builder b = BTreeRow.sortedBuilder();
        b.newRow(METADATA.comparator.make(ck));
        for (Cell<?> c : cells)
            b.addCell(c);
        return b.build();
    }

    private static Cell<?> intCell(ColumnMetadata col, int value, long timestamp)
    {
        return new BufferCell(col,
                              timestamp,
                              Cell.NO_TTL,
                              Cell.NO_DELETION_TIME,
                              ByteBufferUtil.bytes(value),
                              null);
    }
}
