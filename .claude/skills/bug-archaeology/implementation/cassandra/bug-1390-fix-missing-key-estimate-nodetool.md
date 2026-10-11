# Bug 1390: Fix missing key estimate in nodetool

**Commit**: 5d41dab152 · **Ticket**: CASSANDRA-8400
**Subsystem**: tools/NodeTool.java
**Category**: Silent skip on non-match

---

## Root Cause

The `nodetool tablestats` (formerly `cfstats`) output did not include a "Number of keys (estimate)" line even though that metric was available via JMX. The `EstimatedColumnCountHistogram` metric holds a histogram of estimated row counts per SSTable; summing its buckets gives the overall key estimate. The display loop simply never fetched or printed this metric, so the information was silently absent from the output.

The omission was a gap in the implementation: other metrics like `SnapshotsSize` and `CompressionRatio` appeared in the loop, but the key estimate was never wired up.

### Before / After

```java
// BEFORE — key estimate not displayed at all
System.out.println("\t\tSSTable Compression Ratio: " + ...);
System.out.println("\t\tMemtable cell count: " + ...);
```

```java
// AFTER
System.out.println("\t\tSSTable Compression Ratio: " + ...);
int numberOfKeys = 0;
for (long keys : (long[]) probe.getColumnFamilyMetric(keyspaceName, cfName, "EstimatedColumnCountHistogram"))
    numberOfKeys += keys;
System.out.println("\t\tNumber of keys (estimate): " + numberOfKeys);
System.out.println("\t\tMemtable cell count: " + ...);
```

---

## How to Spot

When adding a new metric to the JMX/metrics layer, check that the corresponding display code in nodetool (or any other consumer of those metrics) is updated in the same commit. A checklist of displayed fields compared to exposed metrics helps catch these omissions during review.

---

## Test

No test added.
