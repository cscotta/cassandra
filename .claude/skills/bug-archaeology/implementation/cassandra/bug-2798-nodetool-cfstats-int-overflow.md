# Bug 2798: Integer Overflow in nodetool cfstats Read/Write Count Accumulators

**Commit**: 000e0ff04c · **Ticket**: CASSANDRA-2057

## Detection Rule
When `int` variables accumulate per-column-family read/write counts across many column families in a loop, the sum overflows for heavily-loaded clusters and displays negative or nonsensical totals in nodetool output.

## Code Smell
`int tableReadCount = 0; int tableWriteCount = 0;` inside a loop that sums `getReadCount()` and `getWriteCount()` values — these are themselves `long` JMX attributes. The local accumulators silently truncate the sum to 32 bits.

## Tags
- scope: java
- category: wrong-constant
- code-pattern: int-accumulator-for-long-sum
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause
`NodeCmd.printColumnFamilyStats()` used `int` typed local variables `tableReadCount` and `tableWriteCount` to accumulate total operation counts across all column families in a keyspace. Individual CF read/write counts are `long` values from JMX. For active clusters, the total easily exceeds `Integer.MAX_VALUE` (2.1 billion), causing silent overflow that makes nodetool cfstats report negative totals or wildly wrong percentages. Changing both variables to `long` fixes the overflow.

## Example
```java
// BEFORE: int accumulator overflows for high-traffic tables
int tableReadCount = 0;
int tableWriteCount = 0;
for (ColumnFamilyStoreMBean cfProxy : columnFamilies) {
    tableReadCount += cfProxy.getReadCount();   // long → int truncation
    tableWriteCount += cfProxy.getWriteCount();
}
```
```java
// AFTER: long accumulator
long tableReadCount = 0;
long tableWriteCount = 0;
```
