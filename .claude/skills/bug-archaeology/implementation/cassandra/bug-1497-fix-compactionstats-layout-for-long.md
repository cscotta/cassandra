# Bug 1497: Fix compactionstats layout for long ks/cf names

**Commit**: 4c510dc6cc · **Ticket**: CASSANDRA-7263
**Subsystem**: tools/NodeTool
**Category**: Logic error in condition

---

## Root Cause

The `compactionstats` command in `NodeTool` used a fixed-width `printf` format string (`%25s%16s%16s...`) for every column. When keyspace or table names exceeded the fixed column widths the output columns would overflow into each other, making the table unreadable. The fix computes the actual maximum width of each column across all rows and builds a dynamic format string that accommodates the widest value.

### Before / After

```java
// BEFORE
System.out.printf("%25s%16s%16s%16s%16s%10s%10s%n",
    "compaction type", "keyspace", "table", "completed", "total", "unit", "progress");
for (Map<String, String> c : cm.getCompactions())
{
    System.out.printf("%25s%16s%16s%16s%16s%10s%10s%n",
        c.get("taskType"), c.get("keyspace"), c.get("columnfamily"), ...);
}
```

```java
// AFTER
List<String[]> lines = new ArrayList<>();
int[] columnSizes = new int[] { 0, 0, 0, 0, 0, 0, 0 };
addLine(lines, columnSizes, "compaction type", "keyspace", "table", ...);
for (Map<String, String> c : cm.getCompactions())
    addLine(lines, columnSizes, taskType, keyspace, columnFamily, ...);
// Build format string from observed max column widths
StringBuilder buffer = new StringBuilder();
for (int columnSize : columnSizes) { buffer.append("%").append(columnSize + 3).append("s"); }
String format = buffer.append("%n").toString();
for (String[] line : lines)
    System.out.printf(format, line[0], line[1], line[2], line[3], line[4], line[5], line[6]);
```

---

## How to Spot

Hard-coded fixed-width format strings for tabular CLI output are fragile when field values can be arbitrarily long. Always compute column widths dynamically from the actual data, or use a layout helper that tracks the maximum width seen.

---

## Test

No test added.
