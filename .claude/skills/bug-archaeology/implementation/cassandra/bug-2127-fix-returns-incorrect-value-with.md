# Bug 2127: get_count Terminates Pagination Too Early When TTL-Expired Columns Are Present

**Commit**: 1cbbba095a · **Ticket**: CASSANDRA-5099
**Subsystem**: src/java/org/apache/cassandra/thrift/CassandraServer.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

The Thrift `get_count` implementation paginates through columns by issuing repeated slice reads. It stopped pagination when the last fetched page contained fewer columns than the requested page size (`columns.size() < predicate.slice_range.count`). This assumption breaks when TTL-expired columns exist: a page may be "full" at the storage level but return fewer live columns to the client, causing the loop to terminate prematurely and return a count that is too low. The correct termination condition is that the last page returned only the start marker column that was used to set the next page boundary — meaning there are truly no more columns to count.

### Before / After

```java
// BEFORE — stops when page looks smaller than requested (breaks with TTL expiry)
if (remaining == 0 || columns.size() < predicate.slice_range.count)
    break;
```

```java
// AFTER — stops only when the page contains nothing beyond the start-of-page marker
if (remaining == 0 || ((columns.size() == 1) && (firstName.equals(predicate.slice_range.start))))
    break;
```

---

## How to Spot

Pagination loops that use "page was not full" as the termination sentinel are fragile whenever the data source can silently drop entries (expired TTLs, tombstones, access-control filtering). Always prefer a sentinel that checks whether the response actually advanced past the last-seen position rather than checking response size.

---

## Test

No test added.
