# Bug 2387: KEYS secondary index skips valid results due to wrong page-size accounting

**Commit**: baf1300949 · **Ticket**: CASSANDRA-3996

## Detection Rule

When a pagination loop skips the first entry of a page and decrements `columnsRead`, the loop will incorrectly believe the page is not full and terminate early, dropping valid rows that were never returned.

## Code Smell

A skip-and-continue branch that decrements a "columns read" counter at the same time it advances the iterator — if skipping is not supposed to consume the page budget, the decrement is wrong.

## Tags

- scope: cassandra
- category: off-by-one
- code-pattern: skip-decrement-page-budget
- severity: silent-wrong-result
- subsystem: sai-secondary-index

## Root Cause

`KeysSearcher` uses paged iteration over index entries. When it skips the row already seen at the boundary of the previous page (or a key excluded by range), it called `indexColumns.next()` and also decremented `columnsRead`. The decrement made the loop think it had read fewer entries than the page size, so it stopped paging early and silently dropped results that existed in subsequent pages.

## Example

```java
// BEFORE
if (lastSeenKey.equals(firstColumn.name())) {
    indexColumns.next();
    columnsRead--;   // wrong: reduces page budget when skipping
    logger.debug("Skipping {}", ...);
} else if (range instanceof Range && indexColumns.hasNext() && firstColumn.name().equals(startKey)) {
    indexColumns.next();
    columnsRead--;   // same mistake for range-excluded key
    logger.debug("Skipping first key as range excludes it");
}
```

```java
// AFTER
if (lastSeenKey.equals(firstColumn.name())) {
    indexColumns.next();
    // no columnsRead-- ; skipping does not consume the page quota
    logger.debug("Skipping {}", ...);
} else if (range instanceof Range && indexColumns.hasNext() && firstColumn.name().equals(startKey)) {
    indexColumns.next();
    logger.debug("Skipping first key as range excludes it");
}
```
