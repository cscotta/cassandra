# Bug 2566: cfhistograms output swaps read and write latency columns

**Commit**: 64b4e4f23d · **Ticket**: CASSANDRA-3222

## Detection Rule
When two array variables (`rwlh` for write latency, `rrlh` for read latency) are indexed in swapped positions in a formatted output row, the displayed columns are transposed.

## Code Smell
In a `printf`/`format` call, `rrlh[i]` appears in the write-latency column position and `rwlh[i]` appears in the read-latency column position, reversing the displayed values.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: transposed-array-variables
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause
`NodeCmd.printColumnFamilyHistograms()` formatted each histogram row using `rrlh` (read-request latency histogram) in the write-latency column and `rwlh` (write-request latency histogram) in the read-latency column. The resulting `cfhistograms` output showed write latencies under "Read Latency" and vice versa, silently misleading operators.

## Example
```java
// BEFORE (read/write indices swapped)
output.println(String.format("...",
    offsets[i],
    (i < sprh.length ? sprh[i]  : ""),
    (i < rwlh.length ? rrlh[i]  : ""),  // wrong: rrlh in write column
    (i < rrlh.length ? rwlh[i]  : ""),  // wrong: rwlh in read column
    ...));
```
```java
// AFTER
    (i < rwlh.length ? rwlh[i]  : ""),  // write latency
    (i < rrlh.length ? rrlh[i]  : ""),  // read latency
```
