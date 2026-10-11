# Bug 2935: SSTable index lookup opens data file instead of index file

**Commit**: 3998a4e600 · **Ticket**: n/a

## Detection Rule
When a method that should open a specific component of a multi-file SSTable uses a generic `path` variable instead of calling the component-specific accessor (`indexFilename()`), reads will return garbage data or fail.

## Code Smell
`new BufferedRandomAccessFile(path, "r")` used in the index-reading code path where `path` points to the data file component; the correct call is `new BufferedRandomAccessFile(indexFilename(), "r")`.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: wrong-file-component-opened
- severity: crash
- subsystem: storage-engine

## Root Cause
A bad rebase introduced a regression: the `SSTableReader` code that opens a file to read the index was using `path` (which refers to the SSTable data file) rather than `indexFilename()`. Any SSTable query that fell through to the unbuffered index read path would open and read the data file as if it were the index, producing corrupt results or an exception.

## Example
```java
// BEFORE (regression)
input = new BufferedRandomAccessFile(path, "r");
// path is the data component, not the index
```
```java
// AFTER
input = new BufferedRandomAccessFile(indexFilename(), "r");
// correctly opens the .Index component
```
