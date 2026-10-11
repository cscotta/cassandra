# Bug 0519: Empty Clusterings with kind==CLUSTERING Not Normalized to Clustering.EMPTY

**Commit**: c6544fd200 · **Ticket**: CASSANDRA-15498
**Subsystem**: `db/Clustering.java`, `db/AbstractBufferClusteringPrefix.java`
**Category**: State not cleaned up

---

## Root Cause

`Clustering.make(ByteBuffer... values)` and `ClusteringPrefix.copy()` created new `BufferClustering` instances for zero-length value arrays instead of returning the canonical `Clustering.EMPTY` singleton. This meant that code comparing or interning empty clusterings could encounter multiple distinct objects representing the same logical state, breaking identity-equality assumptions and producing incorrect behavior in range tombstone logic that relied on object identity or `kind()` checks. Additionally, `AbstractBufferClusteringPrefix` defined its own `EMPTY_SIZE` constant by measuring a newly constructed `Clustering.make(EMPTY_VALUES_ARRAY)` — but after the `make` method was fixed to return `Clustering.EMPTY`, the sizing was inconsistent. The fix normalizes all zero-length `CLUSTERING`-kind instances to `Clustering.EMPTY` at construction/copy time, and moves the canonical size constant to `Clustering.EMPTY_SIZE` where it is measured from `BufferClustering` directly.

### Before / After

```java
// BEFORE – Clustering.make and copy() allocate new objects for empty arrays
public static Clustering make(ByteBuffer... values) {
    return new BufferClustering(values);
}
// copy() also creates new BufferClustering for empty non-static clusterings
return kind() == Kind.STATIC_CLUSTERING ? this : new BufferClustering(EMPTY_VALUES_ARRAY);
```

```java
// AFTER – empty clusterings always return the singleton
public static Clustering make(ByteBuffer... values) {
    return values.length == 0 ? EMPTY : new BufferClustering(values);
}
// copy() returns the canonical singleton
return kind() == Kind.STATIC_CLUSTERING ? this : EMPTY;
```

---

## How to Spot

Whenever a factory method or copy constructor accepts a variable-length array, reviewers should ask whether the zero-length case should be a canonical singleton rather than a freshly allocated object. Code that later checks `if (clustering == Clustering.EMPTY)` or relies on `kind()` being consistent with the values array length will silently misbehave if multiple non-identical empty instances exist. A secondary signal is a duplicated `EMPTY_SIZE`-style constant defined in a subclass or abstract class when the correct measure is already (or should be) in the interface.

---

## Test

No test added.
