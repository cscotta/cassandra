# Bug 0193: Missing referencesUserType Override Breaks Schema Loading of UDTs Inside Vectors Inside UDTs

**Commit**: 7d54a62ccc · **Ticket**: CASSANDRA-18964
**Subsystem**: cql3/CQL3Type (Vector raw type)
**Category**: Missing override of default method

---

## Root Cause

`CQL3Type.Raw.Vector` did not override the `referencesUserType(String name)` method. The base implementation in `CQL3Type.Raw` returns `false` by default. When schema loading resolves UDT references (e.g. for `vector<frozen<b>, 2>` inside a UDT `a`), it calls `referencesUserType()` to discover dependencies; because `Vector` always returned `false`, the inner UDT `b` was never flagged as a dependency, causing schema loading to fail or produce an incorrect type graph when UDTs are nested inside vectors inside other UDTs.

### Before / After

```java
// BEFORE – no override in CQL3Type.Raw.Vector
// referencesUserType() inherited, always returns false
```

```java
// AFTER
@Override
public boolean referencesUserType(String name)
{
    return element.referencesUserType(name);
}
```

---

## How to Spot

Whenever a composite `CQL3Type.Raw` subclass wraps another raw type (e.g. `Vector` wrapping its element type, `List` wrapping its value type), every method that must recurse into child types — `referencesUserType`, `referencesDuration`, `udts`, etc. — must be explicitly overridden to delegate to the child. Missing any one of these means the outer container is treated as opaque for that property, silently hiding nested type dependencies from the schema loader.

---

## Test

`CQLVectorTest.sandwichBetweenUDTs` creates UDT `b`, wraps it in `vector<frozen<b>, 2>` inside UDT `a`, then calls `SchemaKeyspace.fetchNonSystemKeyspaces()` to verify the schema round-trips without error.
