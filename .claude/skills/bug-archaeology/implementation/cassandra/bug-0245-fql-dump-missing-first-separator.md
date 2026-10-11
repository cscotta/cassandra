# Bug 0245: FQL Dump Tool Missing Separator Before First Value Entry

**Commit**: b74c86404a · **Ticket**: CASSANDRA-18215
**Subsystem**: tools/fqltool/commands/Dump
**Category**: Logic error in condition

---

## Root Cause

`Dump.appendValuesToStringBuilder()` used a `first` boolean flag to suppress the `"-----"` separator before the very first value. The intent was apparently to avoid a leading separator, but the separator is meant to delimit entries *after* each value so that the output can be parsed by downstream tools. Skipping the separator after the first value meant the first entry had no trailing delimiter, making the output for multi-value queries ambiguous or unreadable by parsers that depend on the separator to locate entry boundaries. The fix removes the `first` flag entirely and unconditionally appends the separator after every value.

### Before / After

```java
// BEFORE
boolean first = true;
for (ByteBuffer value : values) {
    // ... render value ...
    if (first) { first = false; }
    else        { sb.append("-----").append(System.lineSeparator()); }
}
```

```java
// AFTER
for (ByteBuffer value : values) {
    // ... render value ...
    sb.append("-----").append(System.lineSeparator());
}
```

---

## How to Spot

The "skip separator on first element" pattern is correct for *infix* separators (between items). When the separator is a *record terminator* (after each item), applying the skip-first logic produces a missing terminator on the first record. During review, distinguish between separators that belong *between* records and terminators that belong *after* each record; the former uses the skip-first pattern, the latter should not.

---

## Test

A new `testDumpQueryValuesShouldHaveSeperator` unit test in `DumpTest` queries a prepared statement with three bound values and asserts that a `"-----"` separator appears after every value entry in the output.
