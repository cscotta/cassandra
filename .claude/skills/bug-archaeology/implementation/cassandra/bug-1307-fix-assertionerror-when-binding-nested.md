# Bug 1307: AssertionError When Binding Nested Collections in DELETE

**Commit**: bcf0ec6819 · **Ticket**: CASSANDRA-8900
**Subsystem**: cql3/Lists, cql3/Maps, cql3/Sets, cql3/Operation
**Category**: Logic error in condition

---

## Root Cause

The collection mutation classes (`Lists.DiscarderByIndex`, `Maps.DiscarderByKey`, and `Sets.Discarder`) assumed that any bound element extracted from the query options would always be an instance of `Constants.Value`. This assumption holds for simple scalar types, but when a collection type is used as a map key, set element, or list index—i.e. a frozen nested collection—the bound terminal is an instance of `Sets.Value`, `Maps.Value`, `Lists.Value`, or similar, not `Constants.Value`. The `assert` and direct cast guarded by that assumption would throw `AssertionError` or `ClassCastException`. The fix replaces the hardcoded cast with a polymorphic call to `terminal.get(options)` and introduces a new `Sets.ElementDiscarder` operation that correctly handles frozen-collection set elements using the same pattern.

### Before / After

```java
// BEFORE – Maps.DiscarderByKey crashes on nested collection key
assert key instanceof Constants.Value;
CellName cellName = cf.getComparator().create(prefix, column, ((Constants.Value)key).bytes);
```

```java
// AFTER – polymorphic get() works for any terminal type
CellName cellName = cf.getComparator().create(prefix, column, key.get(params.options));
```

```java
// BEFORE – Sets.Discarder assumes Constants.Value or Sets.Value only
Set<ByteBuffer> toDiscard = value instanceof Constants.Value
    ? Collections.singleton(((Constants.Value)value).bytes)
    : ((Sets.Value)value).elements;
```

```java
// AFTER – inverts the check; falls back to get() for any non-Set terminal
Set<ByteBuffer> toDiscard = value instanceof Sets.Value
    ? ((Sets.Value)value).elements
    : Collections.singleton(value.get(params.options));
```

---

## How to Spot

Whenever code narrows a polymorphic `Term.Terminal` to a specific subclass via `assert instanceof` or a direct cast, ask whether every concrete subclass that can appear at that call site has been considered. CQL operations that work on collection elements must handle frozen nested collections (whose bound terminals are collection-typed, not scalar) through the abstract `get(QueryOptions)` API rather than a concrete subtype assumption.

---

## Test

`FrozenCollectionsTest` extended with multiple test cases covering DELETE on frozen lists inside maps/sets/lists, frozen maps inside maps/sets/lists, and frozen sets inside maps/sets/lists—validating both element deletion and the correct resulting state.
