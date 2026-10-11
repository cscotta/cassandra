# Bug 0181: StackOverflowError From Repeated UnmodifiableMap Wrapping

**Commit**: a443990bfa · **Ticket**: CASSANDRA-19166
**Subsystem**: schema/TableMetadataRefCache
**Category**: State not cleaned up

---

## Root Cause

`TableMetadataRefCache` wrapped all three of its internal maps in `Collections.unmodifiableMap` inside its constructor. Every schema change created a new `TableMetadataRefCache` by passing the existing (already-unmodifiable) map instances into the same constructor, which wrapped them again. After many `ALTER` statements the maps became deeply nested chains of `UnmodifiableMap` wrappers, each delegating `get()` to the inner instance. Eventually the delegation chain was deep enough to overflow the call stack. The fix stores the maps unwrapped and only applies `Collections.unmodifiableMap` when the contents actually change (i.e. tables or indexes are created or dropped).

### Before / After

```java
// BEFORE – constructor always wraps, even if maps are already UnmodifiableMap instances
public TableMetadataRefCache(...) {
    this.metadataRefs = Collections.unmodifiableMap(metadataRefs);
    ...
}
return new TableMetadataRefCache(metadataRefs, metadataRefsByName, indexMetadataRefs);
```

```java
// AFTER – store raw maps; only wrap when content changes
private TableMetadataRefCache(...) {
    this.metadataRefs = metadataRefs;   // no unconditional wrap
    ...
}
return new TableMetadataRefCache(
    hasCreatedOrDroppedTablesOrViews ? unmodifiableMap(metadataRefs) : metadataRefs,
    hasCreatedOrDroppedTablesOrViews ? unmodifiableMap(metadataRefsByName) : metadataRefsByName,
    hasCreatedOrDroppedIndexes       ? unmodifiableMap(indexMetadataRefs) : indexMetadataRefs);
```

---

## How to Spot

A constructor that defensively wraps a collection parameter with `Collections.unmodifiableMap` (or similar) without first checking whether the input is already a wrapper will silently build up a delegation chain each time objects are reconstructed from prior instances. Look for patterns where a class is immutably rebuilt from its own fields and the constructor always wraps.

---

## Test

No new test added; the bug manifests only after a large number of schema changes that are not easily exercisable in a short unit test.
