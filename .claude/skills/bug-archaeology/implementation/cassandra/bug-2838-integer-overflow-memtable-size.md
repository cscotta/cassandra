# Bug 2838: Integer overflow computing memtable flush size and timeout

**Commit**: e5df4413b2 · **Ticket**: CASSANDRA-1785

## Detection Rule
When multiplying `int` values to produce a result larger than 2^31-1 (e.g., memsize in MB times 1024*1024), promote to `long` immediately by using a `long` literal (`2L`, `1000L`) as the first operand.

## Code Smell
`2 * memsize.value() * 1024 * 1024` overflows `int` for any memtable size above ~1 GB (memsize > 1024 MB). Similarly, `getMemtableFlushAfterMins() * 60 * 1000` overflows for flush periods above ~35 minutes.

## Tags
- scope: java
- category: off-by-one
- code-pattern: integer-overflow-in-multiplication
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
Both expressions use `int` arithmetic throughout. Java evaluates left-to-right: all intermediate results are `int`, so when the value exceeds `Integer.MAX_VALUE` the result wraps to a negative number. Disk-location selection and flush-timeout checking then use these corrupted values. Promoting the leading constant to `long` (`2L`, `1000L`) forces the entire expression to `long`.

## Example
```java
// BEFORE
long guessedSize = 2 * memsize.value() * 1024*1024;
return System.currentTimeMillis() > creationTime + getMemtableFlushAfterMins() * 60 * 1000;

// AFTER
long guessedSize = 2L * memsize.value() * 1024*1024;
return System.currentTimeMillis() > creationTime + getMemtableFlushAfterMins() * 60 * 1000L;
```
