# Bug 0079: Negative commandStoreId Breaks Topology Update Key Ordering

**Commit**: 21f9f248cd · **Ticket**: CASSANDRA-20219
**Subsystem**: service/accord/AccordJournal.java, JournalKey.java, AccordSegmentCompactor.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`AccordJournal` stored and retrieved topology updates using `commandStoreId = -1` as the sentinel value in `JournalKey`. `JournalKey` serializes the `commandStoreId` as part of its byte-comparable key, and a negative value produces a different byte order than non-negative values, meaning the `DecoratedKey` ordering diverged from the `JournalKey.SUPPORT.compare` ordering. When `AccordSegmentCompactor` wrote compacted SSTable partitions it assumed keys arrived in ascending `DecoratedKey` order; the ordering mismatch caused the compactor to write partitions out of order, corrupting the compacted segment. The fix changes the sentinel to `commandStoreId = 0` (a valid non-negative value), adds an `Invariants.checkArgument(commandStoreId >= 0)` guard in the `JournalKey` constructor, and adds an assertion in `AccordSegmentCompactor` that detects future `DecoratedKey` vs `JournalKey` ordering disagreements.

### Before / After

```java
// BEFORE – uses -1 as sentinel, producing wrong byte-key order
new JournalKey(TxnId.NONE, JournalKey.Type.TOPOLOGY_UPDATE, -1)
```

```java
// AFTER – uses 0, consistent with non-negative key space
new JournalKey(TxnId.NONE, JournalKey.Type.TOPOLOGY_UPDATE, 0)
// plus constructor guard:
Invariants.checkArgument(commandStoreId >= 0);
```

---

## How to Spot

Any key type that is both compared as a Java object and serialized into a byte-comparable representation must ensure that the two orderings are consistent for all valid (and sentinel) values. During review, if negative sentinel values are used in a key that undergoes byte serialization, check whether the byte encoding of negative integers matches their logical comparison order. Also verify that any `maybeWrite` / compaction path that streams keys to an SSTable writer asserts ascending key order so that violations surface early.

---

## Test

`JournalKeySerializerTest.testOrder` added: generates random `JournalKey` pairs and asserts that `DecoratedKey.compareTo` and `JournalKey.SUPPORT.compare` agree on their relative ordering.
