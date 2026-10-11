# Bug 2442: Hadoop ColumnFamilyRecordReader compares Column object instead of column name

**Commit**: b60101c9aa · **Ticket**: CASSANDRA-3788

## Detection Rule

When checking whether iteration has reached a sentinel "nothing new" boundary, verify the comparison is on the correct field of the object (e.g., `.name`) rather than the whole object instance.

## Code Smell

`rows.get(0).columns.get(0).column.equals(startColumn)` compares a `Column` object against a `ByteBuffer`; with no common `equals` override this is always false, disabling the end-of-data detection.

## Tags

- scope: cassandra
- category: wrong-filter-result
- code-pattern: wrong-field-compared
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause

The Hadoop `ColumnFamilyRecordReader` uses the last seen column as a start marker for the next page and detects end-of-data by checking whether the first column of the new page equals the start marker. The comparison incorrectly used `column.equals(startColumn)` where `column` is a `Column` (a Thrift struct) and `startColumn` is a `ByteBuffer`. Java object equality between these different types is always false, so the loop never detected the end of data. The fix accesses the column name field: `column.name.equals(startColumn)`.

## Example

```java
// BEFORE – compares Column struct to ByteBuffer, always false
if (lastRow != null && (rows.get(0).key.equals(lastRow.key)
        || rows.get(0).columns.get(0).column.equals(startColumn)))

// AFTER
if (lastRow != null && (rows.get(0).key.equals(lastRow.key)
        || rows.get(0).columns.get(0).column.name.equals(startColumn)))
```
