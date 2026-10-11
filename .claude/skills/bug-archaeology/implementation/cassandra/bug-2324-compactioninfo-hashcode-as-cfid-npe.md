# Bug 2324: CompactionInfo NPE because cfId lookup via hashCode returns null for system/non-CFS compactions

**Commit**: 6352edb3fe · **Ticket**: CASSANDRA-4318

## Detection Rule

When constructing a metadata object that requires a CFMetaData reference, check whether the lookup can return null (e.g., using `this.hashCode()` as a CF ID to look up in `Schema`) and ensure a non-null value is always provided.

## Code Smell

`new CompactionInfo(this.hashCode(), ...)` — using an arbitrary object hash code as a column family ID to look up schema metadata. A hash code is not a CF ID, so `Schema.instance.getCFMetaData(id)` returns null, making all getKeyspace()/getColumnFamily() calls NPE.

## Tags

- scope: cassandra
- category: missing-null-check
- code-pattern: hashcode-used-as-identifier
- severity: crash
- subsystem: compaction

## Root Cause

`CompactionInfo` had two constructors: one taking `OperationType` only (setting `cfm = null`) and one taking an `Integer id` that performed `Schema.instance.getCFMetaData(id)`. Multiple callers passed `this.hashCode()` as the ID, expecting it to find a valid CFMetaData, but hashCodes are not CF IDs — the lookup returned null. Subsequent calls to `getKeyspace()`, `getColumnFamily()`, and `getId()` all dereferenced the null `cfm` field. The fix changes the constructor to accept `CFMetaData` directly, and updates all call sites to pass the actual metadata object.

## Example

```java
// BEFORE
// In AbstractCompactionIterable:
return new CompactionInfo(this.hashCode(),  // random hash — NOT a valid CF ID
                          type, bytesRead, totalBytes);
// CompactionInfo constructor:
this.cfm = id == null ? null : Schema.instance.getCFMetaData(id);  // returns null
// Later:
public String getKeyspace() { return cfm == null ? null : cfm.ksName; }  // NPE risk
```

```java
// AFTER
return new CompactionInfo(controller.cfs.metadata,  // real CFMetaData
                          type, bytesRead, totalBytes);
// Constructor:
public CompactionInfo(CFMetaData cfm, OperationType tasktype, ...) { this.cfm = cfm; }
public String getKeyspace() { return cfm.ksName; }
```
