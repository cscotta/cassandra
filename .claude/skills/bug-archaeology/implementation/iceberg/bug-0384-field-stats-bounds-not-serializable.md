# Bug 0384: BaseFieldStats bounds not serializable for binary and string types

**Commit**: 48e49443f7113c1d789d8dc588312d4d81687b88 · **Ticket**: #15251

## Detection Rule

When a class that must be serialized stores Java NIO `ByteBuffer` or `CharBuffer` objects as field values, verify that non-serializable buffer types are converted to a serializable representation (e.g. `byte[]` or `String`) at write time and converted back on read.

## Code Smell

A `FieldStats` class whose `lowerBound` / `upperBound` fields are typed as `Object` and may hold `ByteBuffer` (not `Serializable`) or `CharBuffer` (not `Serializable`), passed directly to the builder without conversion — so any attempt to serialize the object throws `NotSerializableException`.

## Tags

- scope: java
- category: wrong-serialization
- code-pattern: mutable-shared-buffer
- severity: crash
- subsystem: core/stats

## Root Cause

`BaseFieldStats` stored lower/upper bounds as whatever Java type `Conversions.fromByteBuffer()` returned, including `ByteBuffer` (for binary/fixed columns) and `CharBuffer` (for string-truncated bounds) — both of which are not `java.io.Serializable`. When `BaseFieldStats` was serialized across an executor boundary, the JVM threw `NotSerializableException`. The fix converts bounds to `byte[]` or `String` at set time via `serializableBound()`, and converts back in `lowerBound()` / `upperBound()`.

## Example

```java
// BEFORE
public Builder<T> lowerBound(T newLowerBound) {
    this.lowerBound = newLowerBound;  // ByteBuffer or CharBuffer — not serializable
    return this;
}
public T lowerBound() { return lowerBound; }
```

```java
// AFTER
private static <T> T serializableBound(T bound) {
    if (bound instanceof CharBuffer) return (T) bound.toString();
    if (bound instanceof ByteBuffer)  return (T) ByteBuffers.toByteArray((ByteBuffer) bound);
    return bound;
}
public Builder<T> lowerBound(T newLowerBound) {
    this.lowerBound = serializableBound(newLowerBound);
    return this;
}
public T lowerBound() {
    if (type.typeId().javaClass().equals(ByteBuffer.class) && lowerBound instanceof byte[])
        return (T) ByteBuffer.wrap((byte[]) lowerBound);
    return lowerBound;
}
```
