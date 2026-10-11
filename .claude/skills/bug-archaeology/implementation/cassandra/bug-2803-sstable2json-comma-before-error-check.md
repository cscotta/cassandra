# Bug 2803: sstable2json Comma Printed Before Skipped (Bad) Rows

**Commit**: d19c9e96c6 · **Ticket**: CASSANDRA-1898

## Detection Rule
When a JSON export loop prints a separator comma unconditionally before checking whether the element was actually written (e.g., after catching and continuing past a serialization error), the output contains dangling commas producing invalid JSON.

## Code Smell
`outs.println(",")` is called inside the `try` block immediately after `serializeRow()`, so if an exception is caught and `continue` is issued, the comma has already been emitted to the output stream before the error is detected.

## Tags
- scope: java
- category: logic-error
- code-pattern: separator-before-error-check
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause
`SSTableExport` printed the row separator (`,`) inside the `try` block right after `serializeRow(outs, row)`. When the row was corrupt or caused `OutOfMemoryError`, the exception was caught and execution `continue`d to the next row — but the comma had already been written, leaving `,..., [corrupt row omitted but comma remains]` in the JSON stream. The fix moves the comma print to after the `try/catch` block and only emits it when `i != 1` (i.e., not the first successfully-written element). In the bulk export path, the same pattern was fixed by using an `elementWritten` flag instead of `continue`.

## Example
```java
// BEFORE: comma printed before error check
try {
    serializeRow(outs, row);
    if (i != 1)
        outs.println(",");   // already emitted even if exception thrown below
} catch (IOException ioexc) {
    System.err.println("ERROR: ...");
    continue;  // too late, comma already in stream
}
```
```java
// AFTER: comma printed only after confirmed success
try {
    serializeRow(outs, row);
} catch (IOException ioexc) {
    System.err.println("ERROR: ...");
    continue;
}
if (i != 1)
    outs.println(",");  // only reached if no exception
```
