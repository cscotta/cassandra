# Bug 1607: Fix Merge-Created Bad Map Key Lookup Type

**Commit**: f64dc45276 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/cql3/statements
**Category**: Wrong constant / default value

---

## Root Cause

`SelectStatement.getRestrictions()` looked up a restriction for `REGULAR` and `STATIC` columns via `stmt.metadataRestrictions.get(def)`, passing the full `CFDefinition.Name` object as the key. However, `metadataRestrictions` is declared as `Map<ColumnIdentifier, Restriction>` and is keyed by `def.name` (a `ColumnIdentifier`), not by `def` itself. Passing the wrong key type always returned `null`, so restriction lookups for regular/static columns silently produced no result. The bug was introduced during a merge and was not caught because Java's `Map.get()` accepts `Object` and simply returns `null` on a type mismatch rather than failing at compile time.

### Before / After

```java
// BEFORE
return stmt.metadataRestrictions.get(def);
```

```java
// AFTER
return stmt.metadataRestrictions.get(def.name);
```

---

## How to Spot

Whenever `Map.get()` is called, verify that the argument type matches the map's declared key type. Java's generics erasure means `Map<K,V>.get(Object)` accepts anything without a compile-time warning, so wrong-type lookups always return `null` silently. Cross-check `get()` call sites against the map's declaration and its `put()` call sites.

---

## Test

No test added.
