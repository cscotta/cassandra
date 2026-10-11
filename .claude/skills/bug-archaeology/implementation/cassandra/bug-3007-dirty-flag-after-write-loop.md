# Bug 3007: dirty_ flag set after loop exits instead of inside write loop, skipping dirty marking

**Commit**: 16f30f1674 · **Ticket**: n/a

## Detection Rule
When a write loop calls a method that writes bytes in chunks, set the dirty flag inside the loop body after each successful chunk write rather than after the loop exits, to ensure dirtiness is recorded even if the loop is later extended or restructured.

## Code Smell
`this.dirty_ = true` appears after a `while (len > 0)` write loop; if the loop body throws or is interrupted, `dirty_` is never set and the buffer is never flushed to disk.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: dirty-flag-after-loop
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
`BufferedRandomAccessFile.write(byte[], int, int)` and `ChecksumRandomAccessFile.write(byte[], int, int)` placed `this.dirty_ = true` after the `while (len > 0)` loop. If the loop terminated early (e.g., exception or future refactoring), the buffer would be left unmarked as dirty, causing `flushBuffer()` to skip the actual write and silently lose data. The fix moves `this.dirty_ = true` inside the loop body, after each call to `writeAtMost()`.

## Example
```java
// BEFORE: dirty set after loop — skipped on early exit
while (len > 0) {
    int n = this.writeAtMost(b, off, len);
    off += n;
    len -= n;
}
this.dirty_ = true; // BUG: not reached on exception

// AFTER: dirty set inside loop after each chunk
while (len > 0) {
    int n = this.writeAtMost(b, off, len);
    off += n;
    len -= n;
    this.dirty_ = true;
}
```
