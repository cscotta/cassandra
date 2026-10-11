# Bug 2914: NPE when reconciler field is null in Avro CassandraServer

**Commit**: 3cc4ee3df2 · **Ticket**: n/a

## Detection Rule
If a field obtained from an Avro/Thrift object is passed directly to a method via `.toString()` without a null check, a NullPointerException occurs when the client omits that optional field.

## Code Smell
Code calls `cfDef.reconciler.toString()` unconditionally on an optional Avro CharSequence field; no null guard before dereferencing.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: unchecked-optional-field-deref
- severity: crash
- subsystem: schema-metadata

## Root Cause
`CassandraServer.system_add_keyspace()` obtained `cfDef.reconciler` from the Avro request and immediately called `.toString()` on it without checking for null. Because `reconciler` is an optional field, clients that don't supply it caused an NPE. The fix extracts the value with a null check before calling `toString()`.

## Example
```java
// BEFORE
AbstractReconciler reconciler = DatabaseDescriptor.getReconciler(
    cfDef.reconciler.toString());  // NPE if reconciler not set
```
```java
// AFTER
reconcilerName = cfDef.reconciler == null ? null : cfDef.reconciler.toString();
AbstractReconciler reconciler = DatabaseDescriptor.getReconciler(reconcilerName);
```
