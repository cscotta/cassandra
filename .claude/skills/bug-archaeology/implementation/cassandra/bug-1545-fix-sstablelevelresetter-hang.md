# Bug 1545: Fix SSTableLevelResetter Hang

**Commit**: e2219ebd8f · **Ticket**: CASSANDRA-7614
**Subsystem**: src/java/org/apache/cassandra/tools/SSTableLevelResetter.java
**Category**: Deadlock

---

## Root Cause

`SSTableLevelResetter` is a standalone tool that calls `Keyspace.openWithoutSSTables`, which starts several daemon threads (e.g., for background operations) that never terminate. Without an explicit `System.exit(0)` call, the JVM hangs waiting for these daemon-like threads to finish. The fix wraps the entire main logic in a try/catch block and always calls `System.exit` at the end, ensuring the process terminates regardless of background thread activity.

### Before / After

```java
// BEFORE
// ... tool logic runs ...
if (!foundSSTable)
{
    out.println("Found no sstables, did you give the correct keyspace/columnfamily?");
}
// method returns normally — JVM hangs on background threads
```

```java
// AFTER
try
{
    // ... tool logic runs ...
    if (!foundSSTable)
        out.println("Found no sstables, did you give the correct keyspace/columnfamily?");
}
catch (Throwable e)
{
    e.printStackTrace();
    System.exit(1);
}
System.exit(0);
```

---

## How to Spot

CLI tools that initialize Cassandra subsystems (via `DatabaseDescriptor.loadSchemas()` or `Keyspace.open*()`) must always call `System.exit` explicitly because those subsystems start non-daemon background threads that will prevent JVM shutdown.

---

## Test

No test added.
