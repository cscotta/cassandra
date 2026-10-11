# Bug 2813: ByteBuffer.get(0) Reads Wrong Byte When Buffer Has Non-Zero Position

**Commit**: 96916e29af · **Ticket**: n/a

## Detection Rule
When `ByteBuffer.get(int index)` is called with a hardcoded index of `0` on a buffer that may have a non-zero `position()` (e.g., a slice or post-read buffer), the code reads from the absolute offset 0 in the backing array rather than the logical first byte of the buffer's content.

## Code Smell
`c.value().get(0) == 1` reads the absolute first byte of the buffer's array, ignoring `position()`. The correct call is `c.value().get(c.value().position())` to read the first byte at the current logical start.

## Tags
- scope: java
- category: off-by-one
- code-pattern: bytebuffer-get-ignores-position
- severity: silent-wrong-result
- subsystem: bootstrap-startup

## Root Cause
`SystemTable.isBootstrapped()` checked whether the bootstrap column's value byte was `1` by calling `c.value().get(0)`. The `ByteBuffer` returned by `c.value()` may have a non-zero `position()` (common when the buffer is backed by a larger array and `position` was set to point to the relevant byte). `ByteBuffer.get(int index)` uses an absolute array index, so `get(0)` reads byte 0 of the backing array rather than the byte at `position`. This makes `isBootstrapped()` return the wrong answer when the backing buffer's position is non-zero, preventing the node from recognizing it has already been bootstrapped.

## Example
```java
// BEFORE: reads wrong byte when position != 0
return c.value().get(0) == 1;
```
```java
// AFTER: reads the byte at the buffer's current logical position
return c.value().get(c.value().position()) == 1;
```
