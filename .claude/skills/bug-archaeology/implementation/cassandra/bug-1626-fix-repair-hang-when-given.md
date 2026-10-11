# Bug 1626: Fix repair hang when given CF does not exist

**Commit**: 359de196f1 · **Ticket**: CASSANDRA-7189
**Subsystem**: service/StorageService (repair path)
**Category**: Silent skip on non-match

---

## Root Cause

`StorageService.getValidColumnFamilies` silently swallowed unknown column family names: when a caller-supplied CF name was not found in the keyspace it logged a warning and called `continue`, producing an empty result set. When the caller was the repair command, the empty set caused it to skip all repair sessions and return normally, so the repair appeared to hang rather than failing with a useful error. The fix removes the silent-skip and lets `Keyspace.getColumnFamilyStore` throw an `IllegalArgumentException` for unknown names, which the repair path now catches and reports back to the client.

### Before / After

```java
// BEFORE – silent skip in getValidColumnFamilies
ColumnFamilyStore cfStore = keyspace.getColumnFamilyStore(baseCfName);
if (cfStore == null)
{
    logger.warn("Invalid column family specified: {}. Proceeding with others.", baseCfName);
    continue;
}
```

```java
// AFTER – null check removed; caller catches IllegalArgumentException
try
{
    Iterables.addAll(columnFamilyStores,
        getValidColumnFamilies(false, false, keyspace, columnFamilies));
}
catch (IllegalArgumentException e)
{
    sendNotification("repair", e.getMessage(),
        new int[]{cmd, ActiveRepairService.Status.FINISHED.ordinal()});
    return;
}
```

---

## How to Spot

A helper method that logs a warning and `continue`s on an invalid input makes failures invisible to callers. Look for patterns where an error condition produces an empty collection without surfacing any exception or status code, especially in paths that then loop over that collection.

---

## Test

No test added.
