# Bug 0778: nodetool compactionstats NPE on null column value

**Commit**: 5d8f6c64c4 · **Ticket**: CASSANDRA-13021
**Subsystem**: Tools / nodetool
**Category**: Missing null / bounds check

---

## Root Cause
`CompactionStats.addLine()` added a `String[]` row to the output table and simultaneously updated `columnSizes[]` by reading `columns[i].length()` without first checking whether the element was `null`. Certain compaction types (notably VALIDATION) can populate compaction info maps with `null` values for fields like `keyspace` or `table`, so when those rows were formatted, a `NullPointerException` was thrown before any output was produced.

### Before / After
```java
// BEFORE
private void addLine(List<String[]> lines, int[] columnSizes, String... columns) {
    lines.add(columns);
    for (int i = 0; i < columns.length; i++) {
        columnSizes[i] = Math.max(columnSizes[i], columns[i].length());
    }
}

// AFTER
private void addLine(List<String[]> lines, int[] columnSizes, String... columns)
{
    String[] newColumns = new String[columns.length];
    for (int i = 0; i < columns.length; i++)
    {
        columnSizes[i] = Math.max(columnSizes[i], columns[i] != null ? columns[i].length() : 1);
        newColumns[i] = columns[i] != null ? columns[i] : "";
    }
    lines.add(newColumns);
}
```

---

## How to Spot
Whenever a method formats rows for a text table by reading `.length()` directly from string array elements that originate from an external map (e.g., `CompactionInfo.toMap()`), ensure every element is null-checked. The fix also separates the mutation of the column-size tracking array from the insertion into the line list, which prevents the original array with null entries from leaking into the output.

---

## Test
No test added.
