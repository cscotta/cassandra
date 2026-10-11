# Bug 1758: Fix count(*) Queries in a Mixed Cluster

**Commit**: 44cf4a66d1 · **Ticket**: CASSANDRA-6707
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java, src/java/org/apache/cassandra/net/MessagingService.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`SelectStatement` unconditionally enabled internal paging for `COUNT(*)` queries when no user page size was set. Internal paging relies on the paging protocol introduced in messaging version 2.0. In a mixed cluster containing pre-2.0 nodes, paging messages would be sent to nodes that did not understand them, causing the count to be computed incorrectly or the query to fail entirely. The fix adds a cluster-wide flag `allNodesAtLeast20` on `MessagingService`, maintained by `setVersion` and `resetVersion`, and gates the automatic count-paging on that flag so it is only activated when every known node can handle paging.

### Before / After

```java
// BEFORE (SelectStatement)
if (parameters.isCount && pageSize <= 0)
    pageSize = DEFAULT_COUNT_PAGE_SIZE;
```

```java
// AFTER (SelectStatement)
if (parameters.isCount && pageSize <= 0 && MessagingService.instance().allNodesAtLeast20)
    pageSize = DEFAULT_COUNT_PAGE_SIZE;
```

```java
// AFTER (MessagingService)
public boolean allNodesAtLeast20 = true;

public int setVersion(InetAddress endpoint, int version) {
    if (version < VERSION_20)
        allNodesAtLeast20 = false;
    ...
    if (v != null && v < VERSION_20 && version >= VERSION_20)
        refreshAllNodesAtLeast20();
    ...
}
```

---

## How to Spot

Features that depend on a minimum protocol version must be guarded by a cluster-wide version check, not merely a local capability check. When adding a protocol-dependent optimization, search for similar "version gate" patterns and ensure the new feature is consistently gated.

---

## Test

No test added.
