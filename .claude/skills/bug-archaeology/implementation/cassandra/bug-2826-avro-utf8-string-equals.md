# Bug 2826: CFMetaData.apply compares Avro Utf8 to String using equals

**Commit**: 493ecaa10a · **Ticket**: CASSANDRA-1962

## Detection Rule
When Avro-generated fields (which use `org.apache.avro.util.Utf8`, not `java.lang.String`) are compared with `.equals()` against Java `String` values, the comparison always returns false; always call `.toString()` first.

## Code Smell
`cf_def.keyspace.equals(tableName)` where `cf_def.keyspace` is an Avro `Utf8` and `tableName` is a Java `String`. Avro's `Utf8.equals(Object)` only returns true for another `Utf8`, so the check always fails and throws a spurious `ConfigurationException("keyspaces do not match")`.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: avro-utf8-string-equals
- severity: crash
- subsystem: schema-metadata

## Root Cause
Avro schema fields (`CfDef.keyspace`, `.name`, `.column_type`) are `CharSequence`/`Utf8` objects. Comparing them directly to Java `String` with `.equals()` never matches. The fix calls `.toString()` on each Avro field before comparison so standard string equality applies.

## Example
```java
// BEFORE
if (!cf_def.keyspace.equals(tableName))
    throw new ConfigurationException("keyspaces do not match.");
if (!cf_def.name.equals(cfName))
    throw new ConfigurationException("names do not match.");

// AFTER
if (!cf_def.keyspace.toString().equals(tableName))
    throw new ConfigurationException("keyspaces do not match.");
if (!cf_def.name.toString().equals(cfName))
    throw new ConfigurationException("names do not match.");
```
