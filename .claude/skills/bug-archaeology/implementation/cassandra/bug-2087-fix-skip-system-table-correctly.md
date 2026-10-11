# Bug 2087: hasExistingNoSystemTables Incorrectly Counted System Table Directory

**Commit**: 564463e40b · **Ticket**: CASSANDRA-5289
**Subsystem**: src/java/org/apache/cassandra/config/DatabaseDescriptor.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`DatabaseDescriptor.hasExistingNoSystemTables()` was supposed to detect whether any non-system data directories existed (to determine whether this was a fresh node). The `FilenameFilter` it used accepted any directory, which included the system keyspace directory (`system/`). As a result, the method returned `true` on nodes that only had system tables on disk, causing incorrect log messages claiming user data tables were present when there were none.

The fix adds a check to exclude the directory whose name equals `Table.SYSTEM_KS` from the count.

### Before / After

```java
// BEFORE
public boolean accept(File pathname)
{
    return pathname.isDirectory();
}
```

```java
// AFTER
public boolean accept(File pathname)
{
    return (pathname.isDirectory() && !Table.SYSTEM_KS.equals(pathname.getName()));
}
```

---

## How to Spot

Methods named `hasExistingNoSystemTables` or similar that list data directories should always explicitly filter out known system/internal directory names. When reviewing directory-scan logic, check whether the filter correctly excludes reserved names rather than relying on callers to do so.

---

## Test

No test added.
