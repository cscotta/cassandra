# Bug 2441: Hadoop Pig validator lookup always returns null due to raw byte array key

**Commit**: a43c1ebbfc · **Ticket**: CASSANDRA-3789

## Detection Rule

When looking up a value in a `Map<ByteBuffer, ...>` using a `byte[]` key, check that the key is wrapped with `ByteBuffer.wrap()` before the lookup; raw `byte[]` uses reference equality not content equality.

## Code Smell

`validators.get(cdef.getName())` where `getName()` returns `byte[]` and `validators` is a `Map<ByteBuffer, ...>`; the lookup always misses because `byte[]` and `ByteBuffer` have incompatible `equals`/`hashCode`.

## Tags

- scope: cassandra
- category: wrong-filter-result
- code-pattern: byte-array-vs-bytebuffer-key
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause

`CassandraStorage` maintained a `Map<ByteBuffer, AbstractType>` for column validators but called `validators.get(cdef.getName())` where `getName()` returns a raw `byte[]`. Java's `Map.get` compares keys using `equals`, and `byte[]` equality is reference-based, not content-based, so the lookup never matched any entry. The validator always fell back to the default marshallers, producing incorrect type mappings for column values in Pig queries. The fix wraps the key: `validators.get(ByteBuffer.wrap(cdef.getName()))`.

## Example

```java
// BEFORE – byte[] key never matches ByteBuffer-keyed map
validator = validators.get(cdef.getName()); // always null

// AFTER
validator = validators.get(ByteBuffer.wrap(cdef.getName()));
```
