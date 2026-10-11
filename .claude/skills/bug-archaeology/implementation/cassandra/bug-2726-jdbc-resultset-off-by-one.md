# Bug 2726: JDBC ResultSet getters use 0-based index instead of JDBC-required 1-based index

**Commit**: 0a54a953d5 · **Ticket**: n/a

## Detection Rule
When implementing a JDBC `ResultSet`, all column-index parameters are 1-based per the JDBC spec; list/array accesses must subtract 1 before indexing into internal 0-based collections.

## Code Smell
`values.get(index)` is called directly in `getBytes`, `getInt`, `getLong`, `getObject`, and `getString` without the required `index - 1` adjustment, causing off-by-one mismatches and `IndexOutOfBoundsException` for the last column.

## Tags
- scope: cassandra
- category: off-by-one
- code-pattern: jdbc-1-based-index-not-adjusted
- severity: crash
- subsystem: tools-clients

## Root Cause
The JDBC specification mandates that column indices are 1-based. The `CassandraResultSet` internal `values` list is 0-based. Every getter that accepted an `int index` parameter used it directly as a list index, so column 1 returned the value at position 1 (the second column), column 1 on a single-column result set returned `IndexOutOfBoundsException`, and the true first column was inaccessible. All five affected getters needed `index - 1`.

## Example
```java
// BEFORE — 0-based access for 1-based JDBC index
public byte[] getBytes(int index) throws SQLException {
    return values.get(index) != null ? ((ByteBuffer)values.get(index).getValue()).array() : null;
}
```
```java
// AFTER — correct 1-to-0 adjustment
public byte[] getBytes(int index) throws SQLException {
    return values.get(index-1) != null ? ((ByteBuffer)values.get(index-1).getValue()).array() : null;
}
```
