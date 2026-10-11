# Bug 0052: Prepared Statement DELETE Timestamp Precedes INSERT, Leaking Entries

**Commit**: d077f69553 · **Ticket**: CASSANDRA-19703
**Subsystem**: src/java/org/apache/cassandra/cql3/QueryProcessor.java, src/java/org/apache/cassandra/db/SystemKeyspace.java
**Category**: Race condition / missing synchronization

---

## Root Cause

Caffeine 2.9.2 can evict a cache entry immediately upon insertion if the cache's maximum weight is already exceeded. The eviction listener called `SystemKeyspace.removePreparedStatement` (a tombstone DELETE) synchronously, potentially before the corresponding `SystemKeyspace.writePreparedStatement` INSERT had been committed with its own wall-clock timestamp. Because both operations used `ClientState.getTimestamp()` at execution time, the DELETE could receive a timestamp equal to or greater than the INSERT, causing the tombstone to survive and the INSERT to be effectively invisible. Over time, `system.prepared_statements` grew unboundedly because evicted statements were never successfully deleted. The fix captures the `Prepared` creation timestamp at construction time and passes it explicitly to `writePreparedStatement` so the INSERT always carries a timestamp strictly earlier than any subsequent DELETE.

### Before / After

```java
// BEFORE – INSERT timestamp derived at write time (could be >= eviction DELETE timestamp)
public static void writePreparedStatement(String loggedKeyspace, MD5Digest key, String cql) {
    executeInternal("INSERT INTO ... (logged_keyspace, prepared_id, query_string) VALUES (?, ?, ?)",
                    loggedKeyspace, key.byteBuffer(), cql);
}
```

```java
// AFTER – INSERT uses the timestamp captured at Prepared creation time
public static void writePreparedStatement(String loggedKeyspace, MD5Digest key, String cql, long timestamp) {
    executeInternal("INSERT INTO ... VALUES (?, ?, ?) USING TIMESTAMP ?",
                    loggedKeyspace, key.byteBuffer(), cql, timestamp);
}
// Prepared stores: public final long timestamp = ClientState.getTimestamp();
```

---

## How to Spot

When a cache eviction listener issues a DELETE to a durable store, and a separate write path issues the corresponding INSERT, both using wall-clock timestamps at time-of-execution, the DELETE can win if the eviction fires immediately after insertion. During review, ensure that a write that must logically precede a future delete captures its timestamp at the time the record is conceptually "created," not at the time the write is executed.

---

## Test

`PstmtPersistenceTest` verifies that for every evicted prepared statement the deletion timestamp recorded in `SystemKeyspace.removePreparedStatement` is strictly greater than the insertion timestamp captured at `Prepared` creation time.
