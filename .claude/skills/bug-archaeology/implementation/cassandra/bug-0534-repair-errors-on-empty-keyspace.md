# Bug 0534: Repair Errors on Empty Keyspace

**Commit**: 5bc7a375a3 · **Ticket**: CASSANDRA-15142
**Subsystem**: `src/java/org/apache/cassandra/repair/RepairRunnable.java`
**Category**: Missing null / bounds check

---

## Root Cause

`RepairRunnable` validated that the keyspace name was recognized and that the requested column families existed, but it did not guard against the case where the resolved set of `validColumnFamilies` was empty (e.g., a keyspace with no tables, or all tables filtered out by user options). The code proceeded into repair logic that assumed at least one table was present, causing errors or exceptions. In Cassandra 2.x this was handled upstream, but the 3.x repair path required its own guard.

### Before / After

```java
// BEFORE: no check for empty column family set; falls through into repair
final long startTime = System.currentTimeMillis();
String message = String.format("Starting repair command #%d (%s), repairing keyspace %s ...", ...);
```

```java
// AFTER: early return with a COMPLETE progress event when no tables exist
if (Iterables.isEmpty(validColumnFamilies))
{
    String message = String.format("Empty keyspace, skipping repair: %s", keyspace);
    logger.info(message);
    fireProgressEvent(new ProgressEvent(ProgressEventType.COMPLETE, 0, 0, message));
    return;
}
```

---

## How to Spot

In any operation that iterates over a set of resources (tables, column families, files), look for whether an empty set is handled before the first use. A missing empty-collection guard at the start of a runnable's main body is a common source of unexpected failures for edge-case inputs that are perfectly valid from the caller's perspective.

---

## Test

No test added.
