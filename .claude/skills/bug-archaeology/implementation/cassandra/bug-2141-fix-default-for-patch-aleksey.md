# Bug 2141: Wrong bloom_filter_fp_chance Default Applied Before Compaction Strategy Is Set

**Commit**: 1772b1c2ec · **Ticket**: CASSANDRA-5093
**Subsystem**: src/java/org/apache/cassandra/cql3/CFPropDefs.java
**Category**: Wrong constant / default value

---

## Root Cause

In `CFPropDefs.applyToCFMetadata()`, the bloom filter false-positive chance was read and applied to the `CFMetaData` object before the compaction strategy was set. Because `CFMetaData.getBloomFilterFpChance()` returns a strategy-specific default (LeveledCompactionStrategy defaults to 0.1, while SizeTiered defaults to 0.01), evaluating it before updating `compactionStrategyClass` meant `getBloomFilterFpChance()` returned the old strategy's default. When a user created a table with LCS without specifying `bloom_filter_fp_chance` explicitly, the SizeTiered default of 0.01 was silently applied instead of LCS's 0.1.

### Before / After

```java
// BEFORE: bloom_filter_fp_chance read before compaction strategy updated
cfm.bloomFilterFpChance(getDouble(KW_BF_FP_CHANCE, cfm.getBloomFilterFpChance()));
if (compactionStrategyClass != null) {
    cfm.compactionStrategyClass(compactionStrategyClass);
    ...
}
```

```java
// AFTER: compaction strategy set first, then bloom filter chance uses correct default
if (compactionStrategyClass != null) {
    cfm.compactionStrategyClass(compactionStrategyClass);
    ...
}
cfm.bloomFilterFpChance(getDouble(KW_BF_FP_CHANCE, cfm.getBloomFilterFpChance()));
```

---

## How to Spot

When a default value is derived from another field (i.e., `getBloomFilterFpChance()` dispatches based on `compactionStrategyClass`), ensure that the dependent field is set first before the derived default is read. Review ordering of setter calls in any `applyTo*` or `update*` method where one property's default depends on another.

---

## Test

No test added.
