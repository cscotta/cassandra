# Bug 2351: CQL3 CREATE TABLE stores column types as strings, causing double-parse and broken counter CF detection

**Commit**: 8dd6a3449a · **Ticket**: CASSANDRA-4185

## Detection Rule

When a builder object stores type information as a `String` (type name) and then parses it back to an `AbstractType` at use time, check whether there is more than one parse site, whether the round-trip is idempotent, and whether type identity checks (e.g., `instanceof CounterColumnType`) are ever performed on the string form.

## Code Smell

`Map<ColumnIdentifier, String> columns` in a DDL statement builder stores column validators as raw strings; at multiple use-sites the string is re-parsed with `CFPropDefs.parseType(str)`, and an `instanceof CounterColumnType` check on the string value silently fails, so counter column families are created as plain CFs.

## Tags

- scope: cassandra
- category: wrong-filter-result
- code-pattern: string-instead-of-typed-value
- severity: silent-wrong-result
- subsystem: schema-metadata

## Root Cause

`CreateColumnFamilyStatement` stored column validators as `String` in its internal map. At use-time, each entry was re-parsed via `CFPropDefs.parseType()`. Two problems arose: (1) the `defaultValidator` for non-compact storage was set to `CFDefinition.definitionType` unconditionally, so counter column families were not recognised — the fix checks `instanceof CounterColumnType` on the actual `AbstractType`, not on the string. (2) Every primary-key type was parsed twice (once at grammar time, once at use-time), wasting work. The fix stores the already-parsed `AbstractType` in the map and removes the redundant second parse.

## Example

```java
// BEFORE
private final Map<ColumnIdentifier, String> columns = new HashMap<>();
// at use-time:
AbstractType<?> validator = CFPropDefs.parseType(col.getValue());
// counter check never worked because type was still a string at check point
stmt.defaultValidator = CFDefinition.definitionType; // always, even for counters
```

```java
// AFTER
private final Map<ColumnIdentifier, AbstractType> columns = new HashMap<>();
// at use-time: no re-parse needed
columnDefs.put(col.getKey().key, new ColumnDefinition(..., col.getValue(), ...));
// counter check now works on the real type:
stmt.defaultValidator = (stmt.columns.values().iterator().next()
        instanceof CounterColumnType) ? CounterColumnType.instance : CFDefinition.definitionType;
```
