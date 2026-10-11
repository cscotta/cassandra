# Bug 1247: Fix ACFIF building of ColumnFamilySplit for non-BO partitioners

**Commit**: 1644e82f75 · **Ticket**: CASSANDRA-9381
**Subsystem**: src/java/org/apache/cassandra/hadoop/AbstractColumnFamilyInputFormat.java
**Category**: Wrong constant / default value

---

## Root Cause

When building `ColumnFamilySplit` objects for Hadoop input splits, the code stripped a two-character prefix (`substring(2)`) from every token's string representation unconditionally. That prefix (`0x`) is present only in the hex-encoded token strings produced by `ByteOrderedPartitioner` and `OrderPreservingPartitioner`; tokens from all other partitioners (e.g. `Murmur3Partitioner`, `RandomPartitioner`) have no such prefix, so `substring(2)` silently corrupted the token string by dropping the first two meaningful characters. Additionally, there was a copy-paste error in the `end` token: it used `subrange.getStart()` instead of `subrange.getEnd()`, meaning every split's end boundary was set to its own start token.

### Before / After

```java
// BEFORE – unconditional substring(2); end token also uses getStart() by mistake
new ColumnFamilySplit(
    subrange.getStart().toString().substring(2),
    subrange.getEnd().toString().substring(2),
    subSplits.get(subSplit),
    endpoints);
```

```java
// AFTER – prefix stripped only for OPP/BOP; end token correctly uses getEnd()
boolean partitionerIsOpp = partitioner instanceof OrderPreservingPartitioner
                        || partitioner instanceof ByteOrderedPartitioner;
new ColumnFamilySplit(
    partitionerIsOpp ? subrange.getStart().toString().substring(2) : subrange.getStart().toString(),
    partitionerIsOpp ? subrange.getEnd().toString().substring(2)   : subrange.getEnd().toString(),
    subSplits.get(subSplit),
    endpoints);
```

---

## How to Spot

Hard-coded magic offsets like `substring(2)` applied to externally-produced strings are a warning sign: the caller must know the exact format contract of the callee. Whenever token or key strings are manipulated with fixed-offset trimming, verify that the format is stable across all configured partitioners. Additionally, any constructor call where adjacent arguments share the same method name (both `getStart()`) should be read carefully — a copy-paste of the first argument where the second was intended is easy to miss.

---

## Test

No test added.
