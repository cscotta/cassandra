# Bug 0720: 2i page size wrong variable checked for zero-column guard

**Commit**: 828ca7cc92 · **Ticket**: CASSANDRA-13400
**Subsystem**: Secondary indexes / query execution
**Category**: Logic error in condition

---

## Root Cause
The page size calculation for secondary index queries divides `meanCellsPerPartition` by `columnsPerRow` to estimate mean rows per partition. A guard was added to avoid a divide-by-zero, but it tested `meanCellsPerPartition <= 0` instead of `columnsPerRow <= 0`. This meant a table with no regular columns (e.g., a table where only the primary key and static columns exist) would trigger the division with `columnsPerRow == 0`, producing an `ArithmeticException`, while the actual statistic being guarded was never zero.

### Before / After
```java
// BEFORE
int columnsPerRow = baseCfs.metadata.partitionColumns().regulars.size();
if (meanCellsPerPartition <= 0)   // wrong variable — should guard columnsPerRow
    return DEFAULT_PAGE_SIZE;

int meanRowsPerPartition = meanCellsPerPartition / columnsPerRow;

// AFTER
int columnsPerRow = baseCfs.metadata.partitionColumns().regulars.size();
if (columnsPerRow <= 0)           // correct: guard the divisor
    return DEFAULT_PAGE_SIZE;

int meanRowsPerPartition = meanCellsPerPartition / columnsPerRow;
```

---

## How to Spot
When two variables are introduced close together and one is immediately used as a divisor, verify that the zero-check guards the divisor, not a different variable that happens to share the same "could be zero" intuition. Here the names `meanCellsPerPartition` and `columnsPerRow` both sound like they could be zero, making it easy to swap the guard. Code review should confirm the guarded expression is the actual denominator.

---

## Test
`CassandraIndexTest` gains a test that creates a table with no regular columns, inserts rows, and executes a secondary index query with paging — verifying that no arithmetic exception is thrown and correct results are returned.
