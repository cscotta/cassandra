# Bug 0614: Repair Trace Event Filter Compares InetAddressAndPort to InetAddress

**Commit**: 0d26879a95 · **Ticket**: n/a
**Subsystem**: `src/java/org/apache/cassandra/repair/RepairRunnable.java`
**Category**: Logic error in condition

---

## Root Cause

The repair progress loop filters out trace events originating from the local node by comparing `source` (an `InetAddressAndPort`) to `r.getInetAddress("source")` (a plain `InetAddress`). Because `InetAddressAndPort.equals(Object)` checks the runtime type of its argument, the comparison always returns `false` when passed an `InetAddress`, so local-node events were never filtered out. The fix extracts the `.address` field from the `InetAddressAndPort` to compare against the `InetAddress` returned by the result row. (This commit is an intermediate fix; CASSANDRA-14225 later extended the comparison to include the port as well.)

### Before / After

```java
// BEFORE
if (source.equals(r.getInetAddress("source")))
    continue;
```

```java
// AFTER
if (source.address.equals(r.getInetAddress("source")))
    continue;
```

---

## How to Spot

When an `equals` comparison involves two objects of different types — here `InetAddressAndPort` vs. `InetAddress` — the call silently returns `false` rather than throwing, meaning the bug surfaces only as wrong runtime behavior. A reviewer should verify that both sides of any `equals` call carry the same type or that one is explicitly unwrapped to match the other.

---

## Test

No test added.
