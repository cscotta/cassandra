# Bug 2306: System-table reads rejected during bootstrap, preventing node startup

**Commit**: e7d3230095 · **Ticket**: n/a (references issue #4329)

## Detection Rule

When a method gates access behind a global state flag (`isBootstrapMode()`, `isShuttingDown()`, `isInitialized()`), check whether the gate also blocks system-internal queries that must succeed regardless of that state.

## Code Smell

A single boolean guard (`if (StorageService.instance.isBootstrapMode())`) blocks all read traffic without distinguishing between client requests and internal system-table queries.

## Tags

- scope: cassandra
- category: wrong-filter-result
- code-pattern: missing-exception-for-internal-queries
- severity: crash
- subsystem: bootstrap-startup

## Root Cause

During bootstrap, `StorageProxy.read()` unconditionally threw `UnavailableException` for every incoming read, including reads of `Table.SYSTEM_TABLE`. System-table reads are used internally for gossip and schema operations that need to succeed before the node is fully bootstrapped, so blocking them caused the node to fail to start up properly.

## Example

```java
// BEFORE
public static List<Row> read(List<ReadCommand> commands, ConsistencyLevel consistency_level)
        throws IOException, UnavailableException, TimeoutException, InvalidRequestException
{
    if (StorageService.instance.isBootstrapMode())
    {
        ClientRequestMetrics.readUnavailables.inc();
        throw new UnavailableException();
    }
    // ...
}
```

```java
// AFTER
private static boolean systemTableQuery(List<ReadCommand> cmds)
{
    for (ReadCommand cmd : cmds)
        if (!cmd.table.equals(Table.SYSTEM_TABLE))
            return false;
    return true;
}

public static List<Row> read(...) throws ... {
    if (StorageService.instance.isBootstrapMode() && !systemTableQuery(commands))
    {
        ClientRequestMetrics.readUnavailables.inc();
        throw new UnavailableException();
    }
    // ...
}
```
