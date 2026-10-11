# Bug 2765: sstable2json Large-Row Pagination Missing Comma Between Pages

**Commit**: b9495d8e8e · **Ticket**: CASSANDRA-2188

## Detection Rule
When paginating through columns of a large row in sstable2json, ensure a comma separator is printed between consecutive pages of JSON output.

## Code Smell
The pagination loop continues fetching the next page after printing columns, but never outputs a comma before the next page begins, producing malformed JSON.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: missing-separator-in-pagination
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause
`SSTableExport.serializeSSTables` paginates large rows by repeatedly fetching `PAGE_SIZE` columns. After detecting that a full page was read (meaning more data exists), the loop continued but omitted a `,` between pages. The resulting JSON stream had adjacent objects with no comma, making it unparseable. The fix adds `out.print(",")` before continuing to the next page.

## Example
```java
// BEFORE
if (columnCount < PAGE_SIZE)
    break;
// loop continues without comma
```
```java
// AFTER
if (columnCount < PAGE_SIZE)
    break;
out.print(",");
// loop continues with comma separating pages
```
