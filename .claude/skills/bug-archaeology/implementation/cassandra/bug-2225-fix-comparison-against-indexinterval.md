# Bug 2225: Off-by-One in SSTableReader Index Interval Scan Boundary

**Commit**: 2a91a48181 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableReader.java
**Category**: Off-by-one

---

## Root Cause

In `SSTableReader.getPosition()`, the loop that scans sampled index positions used a strict less-than (`< IndexInterval`) bound to terminate the scan. This was correct for equality lookups (to exit on bloom-filter false positives), but wrong for range queries: a searched key may be greater than all keys in the current index interval yet still less than the first key of the next interval. The fix changes `< IndexInterval` to `<= IndexInterval` so the next index segment is always examined, ensuring the correct file position is returned for such keys.

### Before / After

```java
// BEFORE
while (segments.hasNext() && i < DatabaseDescriptor.getIndexInterval())
{
    ...
    while (!in.isEOF() && i < DatabaseDescriptor.getIndexInterval())
```

```java
// AFTER
while (segments.hasNext() && i <= DatabaseDescriptor.getIndexInterval())
{
    ...
    while (!in.isEOF() && i <= DatabaseDescriptor.getIndexInterval())
```

---

## How to Spot

When a loop boundary is meant to "check one extra entry beyond the sampled interval" to handle range-query edge cases, using strict less-than instead of less-than-or-equal silently skips that last check. Review index-scan loops where the comment explains that a boundary condition may require examining one further entry.

---

## Test

No test added.
