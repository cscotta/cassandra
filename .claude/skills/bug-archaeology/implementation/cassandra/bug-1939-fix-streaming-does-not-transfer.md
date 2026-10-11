# Bug 1939: Streaming silently skips data in wrapped (ring-crossing) token ranges

**Commit**: 18be7fa876 · **Ticket**: CASSANDRA-5948
**Subsystem**: src/java/org/apache/cassandra/streaming/StreamOut.java
**Category**: Silent skip on non-match

---

## Root Cause

`StreamOut.transferRanges` passed the raw caller-supplied range list directly to `markReferenced` and `transferSSTables` without normalizing it first. On a Cassandra ring a "wrapped" range — one whose start token is greater than its end token, crossing the ring boundary — is internally represented as two separate non-wrapped ranges after normalization. Without calling `Range.normalize`, the wrapped range was used as-is when scanning SSTables, causing those SSTables to match no rows and the data to be silently omitted from the stream.

### Before / After

```java
// BEFORE — raw ranges used throughout
for (Range<Token> range : ranges)
    rowBoundsList.add(range.toRowBounds());
...
transferSSTables(session, sstables, ranges, type);
```

```java
// AFTER — ranges normalized before use
List<Range<Token>> normalizedRanges = Range.normalize(ranges);
for (Range<Token> range : normalizedRanges)
    rowBoundsList.add(range.toRowBounds());
...
transferSSTables(session, sstables, normalizedRanges, type);
```

---

## How to Spot

Any streaming or scanning code that accepts a `List<Range<Token>>` from an external caller should call `Range.normalize` before iterating. The silent-skip symptom — no error, but less data than expected on the destination — is a strong indicator that wrapped ranges were not handled.

---

## Test

`StreamingTransferTest.testTransferTableViaRanges` — a new test path exercises `transferRanges` with a wrapped range and verifies that the expected rows appear on the receiving side.
