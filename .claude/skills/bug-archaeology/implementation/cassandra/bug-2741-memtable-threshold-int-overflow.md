# Bug 2741: Memtable throughput threshold computed in int arithmetic, silently truncates to wrong value

**Commit**: 1331479f16 · **Ticket**: CASSANDRA-2158

## Detection Rule
When computing a threshold in bytes from a megabyte value, multiply using long literals (`1024L * 1024L`) to prevent 32-bit overflow.

## Code Smell
`THRESHOLD = cfs.getMemtableThroughputInMB() * 1024 * 1024` performs `int * int * int` arithmetic; for any throughput >= 2 MB the product overflows a 32-bit signed integer and produces a small or negative threshold, causing the memtable to flush immediately.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: int-overflow-byte-calculation
- severity: performance
- subsystem: storage-engine

## Root Cause
`getMemtableThroughputInMB()` returns an `int`. Multiplying two `int` literals `1024 * 1024` overflows for any value >= 2048 MB before the result is assigned to the `long THRESHOLD` field. Even for typical values like 64 MB, `64 * 1024 * 1024 = 67108864` fits in int, but for larger configurations (>= 2048 MB) the product wraps. Using `1024L * 1024L` promotes the arithmetic to `long` before overflow can occur.

## Example
```java
// BEFORE — int arithmetic overflows for large MB values
THRESHOLD = cfs.getMemtableThroughputInMB() * 1024 * 1024;
```
```java
// AFTER — long arithmetic prevents overflow
THRESHOLD = cfs.getMemtableThroughputInMB() * 1024L * 1024L;
```
