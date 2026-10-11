# Bug 2089: hasExistingNoSystemTables Counts System Keyspace as User Data (1.1 Backport)

**Commit**: 3a6125db87 · **Ticket**: CASSANDRA-5289
**Subsystem**: src/java/org/apache/cassandra/config/DatabaseDescriptor.java
**Category**: Logic error in condition

---

## Root Cause

Same bug as the trunk fix: `DatabaseDescriptor.hasExistingNoSystemTables` used a `FileFilter` that accepted every directory in the data path, including the `system` keyspace directory, causing false-positive "Found table data in data directories" warnings on nodes that only had system tables. This commit applies the same one-line filter fix to the 1.1 branch.

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

When a filter is described as "skipping system tables", confirm the exclusion condition is present in the code, not just implied by the method name. Method naming can mask missing exclusion logic.

---

## Test

No test added.
