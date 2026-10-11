**Commit**: 4a8a0637e0 · **Ticket**: KAFKA-18723
**Subsystem**: core / raft / KafkaRaftClient; core / replication / ReplicaFetcherThread
**Category**: Race condition / missing synchronization

**Root cause**
There is a race between the network thread (which reads bytes from log segments for fetch responses) and the KRaft driver / replica-fetcher thread (which truncates and appends to the log). This can cause the network thread to send record batches whose `partitionLeaderEpoch` is higher than the epoch recorded in the fetch request — i.e., inconsistent records from a future epoch. Previously, only CRC-corrupted records were filtered; epoch-inconsistent ones were silently accepted by the follower, which could then replay wrong data.

**Before/After**
```java
// Before — only corrupt records were guarded
try { appendRecords(...); } catch (CorruptRecordException e) { ... }

// After — also filter batches whose epoch exceeds the fetching replica's epoch
// (see ReplicaFetcherThread and KafkaRaftClient filter logic added by the commit)
// partitionLeaderEpoch added to DefaultRecordBatch.toString() for diagnostics
```

**How to spot**
In replication code, whenever records fetched from the leader are appended to a follower log, check that each batch's `partitionLeaderEpoch` does not exceed the epoch recorded in the fetch request. A batch with a higher epoch is from a newer leader and must be discarded to prevent epoch contamination.

**Test**
Property-based tests (`ArbitraryMemoryRecords`, `InvalidMemoryRecordsProvider`) using jqwik verify that `MemoryRecords` with random/invalid bytes, or with epoch fields exceeding the expected bound, are correctly rejected.
