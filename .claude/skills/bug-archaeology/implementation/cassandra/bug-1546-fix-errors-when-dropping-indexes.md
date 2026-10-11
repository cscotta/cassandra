# Bug 1546: Fix Errors When Dropping Non-Existent Indexes

**Commit**: dceed39995 · **Ticket**: CASSANDRA-7590
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/DropIndexStatement.java
**Category**: Missing null / bounds check

---

## Root Cause

`DropIndexStatement.findIndexedCF()` called `Schema.instance.getKSMetaData(keyspace())` without checking whether the returned `KSMetaData` was null before iterating over it. When the keyspace did not exist, this caused a NullPointerException. Additionally, when `IF EXISTS` was specified and the index was not found, `announceMigration` would return `null` for the table reference (`indexedCF`), but the standard `execute` path would still try to build a `SchemaChange` event from that null reference, causing an `AssertionError`. The fix adds a null check for the keyspace and overrides `execute` to return `null` (no-op) when `indexedCF` is null.

### Before / After

```java
// BEFORE
private CFMetaData findIndexedCF() throws InvalidRequestException
{
    KSMetaData ksm = Schema.instance.getKSMetaData(keyspace());
    // NPE if ksm == null
    for (CFMetaData cfm : ksm.cfMetaData().values())
    { ... }
    if (ifExists)
        return null;
    else
        throw new InvalidRequestException(...);
}
```

```java
// AFTER
private CFMetaData findIndexedCF() throws InvalidRequestException
{
    KSMetaData ksm = Schema.instance.getKSMetaData(keyspace());
    if (ksm == null)
        throw new KeyspaceNotDefinedException("Keyspace " + keyspace() + " does not exist");
    for (CFMetaData cfm : ksm.cfMetaData().values())
    { ... }
    if (ifExists)
        return null;
    else
        throw new InvalidRequestException(...);
}

@Override
public ResultMessage execute(QueryState state, QueryOptions options) throws RequestValidationException
{
    announceMigration(false);
    return indexedCF == null ? null : new ResultMessage.SchemaChange(changeEvent());
}
```

---

## How to Spot

Any method that calls `Schema.instance.getKSMetaData()` must null-check the result before dereferencing it — the keyspace may not exist. Schema-altering statements with `IF EXISTS` semantics must also guard against null table references before constructing response events.

---

## Test

No test added.
