# Bug 1293: NPE When Deleting/Setting by Index on Null List Collection

**Commit**: cc672f3615 · **Ticket**: CASSANDRA-9077
**Subsystem**: src/java/org/apache/cassandra/cql3/Lists.java
**Category**: Missing null / bounds check

---

## Root Cause

The `Lists.Setter`, `Lists.Discarder`, and `Lists.DiscarderByIndex` execution paths all called `params.getPrefetchedList(rowKey, column.name)` and then immediately used the returned list without checking for `null`. When a row exists but its list column has never been set (or was explicitly set to `null` via `DELETE l FROM ...`), `getPrefetchedList` returns `null`. The subsequent call to `existingList.size()` (or iteration over `existingList`) then threw a `NullPointerException`, which surfaced as an unhandled server error rather than an informative client exception.

### Before / After

```java
// BEFORE — no null guard before use
List<Cell> existingList = params.getPrefetchedList(rowKey, column.name);
int idx = ByteBufferUtil.toInt(index);
if (idx < 0 || idx >= existingList.size())  // NPE if existingList is null
```

```java
// AFTER — null checked before access
List<Cell> existingList = params.getPrefetchedList(rowKey, column.name);
int idx = ByteBufferUtil.toInt(index);
if (existingList == null)
    throw new InvalidRequestException("Attempted to set an element on a list which is null");
if (idx < 0 || idx >= existingList.size())
```

---

## How to Spot

Any method that operates on a collection column fetched from a pre-fetch cache should treat the `null` return as a first-class case, not as an impossible state. The pattern to watch for is: `List<X> items = getPrefetched(...); items.size()` with no intervening null check. A broader heuristic: whenever a mutation operation (set-by-index, delete-by-index) acts on data read from a cache or read-before-write step, all three cases—missing, empty, and null—must be explicitly handled.

---

## Test

`CollectionsTest` was extended to assert that `DELETE l[0]`, `UPDATE ... SET l[0] = ?`, and `UPDATE ... SET l = l - ?` on a null list column all raise `InvalidRequestException` with an informative message.
