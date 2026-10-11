# Bug 0457: Serialization cache not invalidated after field removal

**Commit**: 7bead8ab72639f1087b337bbc71bf64a59d058ea · **Ticket**: #15097

## Detection Rule

When a mutable object caches a derived serialized form AND exposes a mutation method, verify that every mutation method invalidates the cache, not just the ones that add or update data.

## Code Smell

A class that nulls out a `serializationState` field inside `put()` but not inside `remove()`, while both methods mutate the logical state that the serialization captures.

## Tags

- scope: universal
- category: state-not-cleaned
- code-pattern: stale-state-after-mutation
- severity: silent-wrong-result
- subsystem: core/variants

## Root Cause

`ShreddedObject` lazily builds a `SerializationState` that encodes the current set of fields. The `put()` method correctly nulls the cache on every write, but `remove()` only updated `shreddedFields` and `removedFields` without clearing `serializationState`. Any serialization performed before the first `remove()` call was therefore reused verbatim on subsequent `sizeInBytes()` or `writeTo()` calls, silently producing output that still contained the deleted field.

## Example

```java
// BEFORE
public void remove(String field) {
  shreddedFields.remove(field);
  removedFields.add(field);
  // serializationState NOT cleared — stale cache returned on next writeTo()
}
```

```java
// AFTER
public void remove(String field) {
  shreddedFields.remove(field);
  removedFields.add(field);
  this.serializationState = null;  // invalidate so next serialize reflects removal
}
```
