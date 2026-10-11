# Bug 2341: FBUtilities.hash ignores arrayOffset, hashing wrong bytes in sliced ByteBuffers

**Commit**: 783e5d4c5f · **Ticket**: CASSANDRA-4250

## Detection Rule

When calling `MessageDigest.update(byte[], offset, length)` on a `ByteBuffer`'s backing array, always add `arrayOffset()` to the position, since a sliced or wrapped ByteBuffer's `position()` is relative to the view, not the underlying array.

## Code Smell

`messageDigest.update(block.array(), block.position(), block.remaining())` — missing `block.arrayOffset()` in the offset argument, causing the digest to hash bytes starting at the wrong position in the backing array.

## Tags

- scope: java
- category: off-by-one
- code-pattern: missing-array-offset-in-bytebuffer
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause

`FBUtilities.hash()` computed a digest over a list of `ByteBuffer` values. For heap-backed buffers with `hasArray()`, it called `messageDigest.update(block.array(), block.position(), block.remaining())`. However, `block.position()` is the view-relative position within the ByteBuffer, not the offset into the backing `byte[]`. For sliced or duplicate buffers, `arrayOffset()` can be non-zero, so the actual start of valid data in the backing array is `arrayOffset() + position()`. Without adding `arrayOffset()`, the hash input was computed over the wrong byte range.

## Example

```java
// BEFORE
if (block.hasArray())
    messageDigest.update(block.array(), block.position(), block.remaining());
```

```java
// AFTER
if (block.hasArray())
    messageDigest.update(block.array(), block.arrayOffset() + block.position(), block.remaining());
```
