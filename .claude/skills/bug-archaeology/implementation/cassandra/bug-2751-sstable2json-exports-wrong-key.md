# Bug 2751: sstable2json Exports Adjacent Row When Requested Key Does Not Exist

**Commit**: f516cc8452 · **Ticket**: CASSANDRA-2168

## Detection Rule
When exporting a specific key from an SSTable, verify that the row returned by the scanner actually matches the requested key before serializing it, since SSTable scanners position to the nearest key rather than an exact match.

## Code Smell
After `scanner.next()`, the code immediately calls `serializeRow` without checking whether `row.getKey().equals(decoratedKey)`; this outputs whatever row the scanner happened to land on.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: missing-key-equality-check
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause
`SSTableScanner` positions to the first key >= the target; if the exact key is absent, the scanner returns the next key in sort order. `SSTableExport.export(SSTableReader, PrintStream, String[])` called `scanner.next()` and serialized the result without confirming the key matched the requested one, silently outputting a different row. The fix adds `if (!row.getKey().equals(decoratedKey)) continue;`.

## Example
```java
// BEFORE
if (!scanner.hasNext())
    continue;
serializeRow(reader, (SSTableIdentityIterator) scanner.next(), decoratedKey, outs);
```
```java
// AFTER
if (!scanner.hasNext())
    continue;
SSTableIdentityIterator row = (SSTableIdentityIterator) scanner.next();
if (!row.getKey().equals(decoratedKey))
    continue;
serializeRow(reader, row, decoratedKey, outs);
```
