# Bug 0552: nodetool cfstats Prints Index Name Twice

**Commit**: f0aef2c54d · **Ticket**: CASSANDRA-14903
**Subsystem**: `src/java/org/apache/cassandra/tools/nodetool/stats/TableStatsPrinter.java`
**Category**: Logic error in condition

---

## Root Cause

The table-stats printer used a ternary expression to label index tables, but appended `table.name` unconditionally after the ternary. When `table.isIndex` was true, the ternary produced `" (index): " + table.name`, and then `table.name` was concatenated again, resulting in the index name appearing twice. The fix was to remove `table.name` from the true-branch of the ternary and let the unconditional concatenation at the end supply it.

### Before / After

```java
// BEFORE
out.println("\t\tTable" + (table.isIndex ? " (index): " + table.name : ": ") + table.name);
```

```java
// AFTER
out.println("\t\tTable" + (table.isIndex ? " (index): " : ": ") + table.name);
```

---

## How to Spot

When a ternary is used to vary only a label/prefix and a value is appended unconditionally after the closing parenthesis, check whether the same value also appears inside any branch of the ternary. The redundant copy is easy to miss because both occurrences are `table.name` and the string just looks like a formatted label at a glance.

---

## Test

No test added.
