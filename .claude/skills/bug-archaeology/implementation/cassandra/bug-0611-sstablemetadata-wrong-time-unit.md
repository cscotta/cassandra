# Bug 0611: SSTableMetadataViewer Displays Wrong Date for minLocalDeletionTime

**Commit**: 714703a08d · **Ticket**: CASSANDRA-14132
**Subsystem**: `src/java/org/apache/cassandra/tools/SSTableMetadataViewer.java`
**Category**: Wrong constant / default value

---

## Root Cause

`EncodingStats.minLocalDeletionTime` is a Unix timestamp stored in **seconds** (consistent with Cassandra's `localDeletionTime` convention), but the viewer passed `TimeUnit.MILLISECONDS` to `toDateString()`. The helper then multiplied the value by 1,000 before converting to a `Date`, producing a timestamp roughly 1,000× too far in the future and making the human-readable date meaningless for any non-trivial deletion time.

### Before / After

```java
// BEFORE
field("EncodingStats minLocalDeletionTime",
      encodingStats.minLocalDeletionTime,
      toDateString(encodingStats.minLocalDeletionTime, TimeUnit.MILLISECONDS));
```

```java
// AFTER
field("EncodingStats minLocalDeletionTime",
      encodingStats.minLocalDeletionTime,
      toDateString(encodingStats.minLocalDeletionTime, TimeUnit.SECONDS));
```

---

## How to Spot

Whenever a diagnostic tool formats a timestamp, check whether the stored value is in seconds or milliseconds and verify it matches the `TimeUnit` argument passed to the formatter. A quick sanity check is to run the tool on a known SSTable and compare the date to the wall clock; a date thousands of years in the future indicates a milliseconds-vs-seconds mismatch.

---

## Test

No test added.
