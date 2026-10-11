# Bug 2088: hasExistingNoSystemTables Counts System Keyspace as User Data

**Commit**: aadc5e500d · **Ticket**: CASSANDRA-5289
**Subsystem**: src/java/org/apache/cassandra/config/DatabaseDescriptor.java
**Category**: Logic error in condition

---

## Root Cause

`DatabaseDescriptor.hasExistingNoSystemTables` was supposed to detect whether any non-system data directories existed on startup. Its `FileFilter` accepted any directory as a match, which meant the `system` keyspace directory was counted as user table data. This caused a misleading log warning "Found table data in data directories" to fire even when a freshly initialized node had only the system keyspace on disk.

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

When a filter or predicate is described as "excluding system X", verify that the exclusion is explicitly coded — the absence of an exclusion clause is easy to miss during review.

---

## Test

No test added.
