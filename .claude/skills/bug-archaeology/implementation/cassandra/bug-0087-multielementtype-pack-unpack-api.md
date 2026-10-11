# Bug 0087: MultiElementType pack/unpack Abstract Methods Missing ValueAccessor Overload

**Commit**: 94c2ec3c96 · **Ticket**: n/a
**Subsystem**: db/marshal/MultiElementType, cql3/selection
**Category**: Missing override of default method

---

## Root Cause

`MultiElementType` declared `pack(List<ByteBuffer>)` and `unpack(ByteBuffer)` as abstract methods, and every concrete subtype implemented only those `ByteBuffer`-specific signatures. A generic `pack(List<V>, ValueAccessor<V>)` / `unpack(V, ValueAccessor<V>)` API was added to support non-`ByteBuffer` representations (e.g. `byte[]`), but the abstract contract was not updated to make that generic form the primary method. As a result, callers used the non-generic overloads even when they had a `ValueAccessor` in hand, bypassing the more general code path and causing incorrect behavior when accessor semantics differed from raw `ByteBuffer` operations. The fix promotes the generic form to the abstract method and provides concrete `ByteBuffer` and `byte[]` convenience wrappers.

### Before / After

```java
// BEFORE – abstract on ByteBuffer-specific signatures
public abstract ByteBuffer pack(List<ByteBuffer> elements);
public abstract List<ByteBuffer> unpack(ByteBuffer value);
```

```java
// AFTER – abstract on generic accessor form; ByteBuffer overloads delegate
public abstract <V> V pack(List<V> elements, ValueAccessor<V> accessor);
public ByteBuffer pack(List<ByteBuffer> elements) {
    return pack(elements, ByteBufferAccessor.instance);
}
public abstract <V> List<V> unpack(V value, ValueAccessor<V> accessor);
public final List<ByteBuffer> unpack(ByteBuffer value) {
    return unpack(value, ByteBufferAccessor.instance);
}
```

---

## How to Spot

When a class hierarchy introduces a generic accessor-parameterized API alongside an existing concrete-type API, verify that the abstract/interface boundary is placed on the generic form so subclasses cannot accidentally implement only the legacy overload. Every call site should be audited to ensure it passes the accessor it actually has rather than relying on a default.

---

## Test

No test added.
