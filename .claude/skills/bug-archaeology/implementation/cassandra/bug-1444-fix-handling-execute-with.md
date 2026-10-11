# Bug 1444: Fix Handling of EXECUTE With skip_metadata=false

**Commit**: 63cb95e012 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/cql3/ResultSet.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`ResultSet.Metadata.copy()` returned a shallow copy that shared the same mutable `EnumSet<Flag>` instance with the original. When an `EXECUTE` request arrived with `skip_metadata=false`, the response path called `copy()` and then added `Flag.NO_METADATA` to the copy's flag set — which silently mutated the original's flags as well. On the next execution of the same prepared statement, the cached `Metadata` now had `NO_METADATA` set, causing the server to omit column metadata even for clients that explicitly requested it.

### Before / After

```java
// BEFORE
public Metadata copy()
{
    return new Metadata(flags, names, columnCount, pagingState);
}
```

```java
// AFTER
public Metadata copy()
{
    return new Metadata(EnumSet.copyOf(flags), names, columnCount, pagingState);
}
```

---

## How to Spot

Any `copy()` or `clone()` method that passes a mutable collection field directly to a new instance constructor rather than making a defensive copy of that collection is a shared-mutable-state bug waiting to happen. `EnumSet`, `HashSet`, `ArrayList`, and similar types passed by reference will silently propagate mutations back to the original. Review every `copy()` method to confirm that mutable fields receive their own independent copy.

---

## Test

`PreparedStatementsTest.testStatementRePreparationOnReconnect` — prepares statements, reconnects to get a fresh session, re-prepares, executes, and asserts the result set is non-empty, which would fail if metadata were incorrectly suppressed.
