# Bug 3027: SSTableSimpleUnsortedWriter buffer size overflows int multiplication

**Commit**: 6ef7cfc386 · **Ticket**: CASSANDRA-3027

## Detection Rule
When computing a byte count by multiplying megabytes by 1024 twice, ensure at least one operand is a `long` literal to prevent int overflow before the result is stored in a `long` field.

## Code Smell
`this.bufferSize = bufferSizeInMB * 1024 * 1024` performs int multiplication that overflows for `bufferSizeInMB >= 2048` even though `bufferSize` is declared `long`.

## Tags
- scope: cassandra
- category: off-by-one
- code-pattern: int-multiplication-overflow-to-long
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause
`SSTableSimpleUnsortedWriter` stored the buffer size as a `long`, but the constructor computed it with `bufferSizeInMB * 1024 * 1024` — three `int` operands. For any `bufferSizeInMB` value large enough the intermediate product overflows to a negative number before being widened to `long`, resulting in an effective buffer size much smaller than intended. The fix changes to `bufferSizeInMB * 1024L * 1024L` to force `long` arithmetic from the first multiplication.

## Example
```java
// BEFORE
this.bufferSize = bufferSizeInMB * 1024 * 1024;  // int overflow possible
```
```java
// AFTER
this.bufferSize = bufferSizeInMB * 1024L * 1024L;  // long arithmetic
```
