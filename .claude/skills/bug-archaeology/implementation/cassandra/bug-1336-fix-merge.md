# Bug 1336: Bad Merge Leaves Bloom Filter Unclosed in SSTableSimple Writers

**Commit**: 2a167ea21e · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableSimpleUnsortedWriter.java, SSTableSimpleWriter.java
**Category**: Resource leak

---

## Root Cause

A bad merge reintroduced `writer.close()` (no-arg) in place of `writer.close(true)` in both `SSTableSimpleWriter` and `SSTableSimpleUnsortedWriter`. The no-arg overload does not close the bloom filter (`iwriter.bf`), so off-heap memory held by the filter is never released. This commit corrects the merge by restoring the `close(true)` calls that the memory-leak fix (CASSANDRA-8748) had introduced.

### Before / After

```java
// BEFORE (bad merge state)
writer.close();
```

```java
// AFTER (restored)
writer.close(true);
```

---

## How to Spot

When a parameterised overload is introduced to fix a resource leak, check every call site that uses the old no-arg form. Merge conflicts often resolve to the no-arg form because it existed in both sides of the merge; a reviewer should verify that all call sites that should pass `true` (or any non-default argument) actually do so after a merge is resolved.

---

## Test

No test added.
