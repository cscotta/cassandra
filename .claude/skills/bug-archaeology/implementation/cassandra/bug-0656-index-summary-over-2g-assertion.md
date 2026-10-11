# Bug 0656: IndexSummary 2 GB Capacity Exceeded Causes Assertion Error

**Commit**: ae88fd6c79 · **Ticket**: CASSANDRA-12014
**Subsystem**: src/java/org/apache/cassandra/io/sstable/IndexSummaryBuilder.java
**Category**: Off-by-one

---

## Root Cause

The overflow check in `IndexSummaryBuilder` compared the raw *entry count* against `Integer.MAX_VALUE`, but the actual memory limit is on the total *byte size* of the summary entries. Each entry occupies the key size plus 8 bytes for the file position (a `long`), so a table with many large keys could exceed 2 GB of index summary memory while having far fewer than `Integer.MAX_VALUE` entries — causing a hard assertion failure inside `maybeAddEntry`. The original initializer also hardcoded 40 bytes per entry (`40 * maxExpectedEntries`) rather than computing the actual per-entry size dynamically from the key length.

### Before / After

```java
// BEFORE
long maxExpectedEntries = expectedKeys / minIndexInterval;
if (maxExpectedEntries > Integer.MAX_VALUE)
{
    int effectiveMinInterval = (int) Math.ceil((double) Integer.MAX_VALUE / expectedKeys);
    maxExpectedEntries = expectedKeys / effectiveMinInterval;
    assert maxExpectedEntries <= Integer.MAX_VALUE : maxExpectedEntries;
}
entries = new SafeMemoryWriter(40 * maxExpectedEntries)...;
// and later:
assert entries.length() <= Integer.MAX_VALUE;
```

```java
// AFTER
long expectedEntrySize = getEntrySize(defaultExpectedKeySize);
long maxExpectedEntries = expectedKeys / minIndexInterval;
long maxExpectedEntriesSize = maxExpectedEntries * expectedEntrySize;
if (maxExpectedEntriesSize > Integer.MAX_VALUE)
{
    int effectiveMinInterval = (int) Math.ceil((double)(expectedKeys * expectedEntrySize) / Integer.MAX_VALUE);
    maxExpectedEntries = expectedKeys / effectiveMinInterval;
    maxExpectedEntriesSize = maxExpectedEntries * expectedEntrySize;
    assert maxExpectedEntriesSize <= Integer.MAX_VALUE : maxExpectedEntriesSize;
}
entries = new SafeMemoryWriter(expectedEntrySize * maxExpectedEntries)...;
// and later: graceful log error instead of assert
```

---

## How to Spot

Whenever code enforces an upper bound on a compound structure by checking only the element *count* rather than the element *count × element size*, it is vulnerable to this category of off-by-one/off-by-scale error. Reviewers should verify that capacity checks are expressed in the same units as the limit being enforced (bytes vs. entries), and that any hardcoded per-element byte sizes are derived from the actual serialization of the element rather than from a historical assumption.

---

## Test

The added test constructs an `IndexSummaryBuilder` with a very low `minIndexInterval` against a large key count to verify that the builder gracefully downsizes the interval rather than asserting when the projected summary would exceed 2 GB.
