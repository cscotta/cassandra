# Bug 0527: SchemaUpdate case-insensitive mode failed to resolve newly added column names for move operations

**Commit**: a1c1c1b5a855a0ce63785f62660cf2490765d4e0 · **Ticket**: #14734

## Detection Rule

When a component stores internally-keyed state in a map and supports a case-insensitive lookup mode, verify that keys are normalized (lowercased) both at insertion time and at lookup time, not just at one of the two sites.

## Code Smell

A map keyed by string field names where inserts use the raw name (`addedNameToId.put(fullName, ...)`) but lookups call a helper that normalizes the key — or vice versa — causing case-insensitive lookups to miss entries that were stored under a different case.

## Tags

- scope: java
- category: logic-error
- code-pattern: inconsistent-map-key-normalization
- severity: silent-wrong-result
- subsystem: core/SchemaUpdate

## Root Cause

`SchemaUpdate` maintains an `addedNameToId` map to track in-flight column additions so that subsequent operations (moves, updates) can reference them before the schema is committed. When `caseSensitive=false`, field lookups used `schema.caseInsensitiveFindField()`, but newly added field names were stored in `addedNameToId` under the original mixed-case name. A `moveFirst("dAtA")` call after `addColumn("data", ...)` would lowercase "dAtA" for the schema lookup but then look for "dAtA" verbatim in `addedNameToId`, finding nothing. The fix normalizes the key to lowercase at both insertion and lookup when `caseSensitive=false`.

## Example

```java
// BEFORE
// Insert: stores under original case
addedNameToId.put(fullName, newId);  // e.g. "data"

// Lookup (in isAdded, findForUpdate, findForMove):
return addedNameToId.containsKey(name);  // "dAtA" — case mismatch, returns false
```

```java
// AFTER
// Insert: normalizes key
addedNameToId.put(caseSensitivityAwareName(fullName), newId);  // "data"

// Lookup: normalizes key the same way
return addedNameToId.containsKey(caseSensitivityAwareName(name));  // "data" — matches

private String caseSensitivityAwareName(String name) {
  return caseSensitive ? name : name.toLowerCase(Locale.ROOT);
}
```
