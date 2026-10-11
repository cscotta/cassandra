# Bug 0246: NPE in FQL Dump Tool When Bound Value Is Null

**Commit**: ba434b8231 · **Ticket**: CASSANDRA-18113
**Subsystem**: tools/fqltool/commands/Dump
**Category**: Missing null / bounds check

---

## Root Cause

`Dump.appendValuesToStringBuilder()` called `Bytes.wrapForRead(value)` unconditionally for every element in the `values` list. CQL prepared statements allow binding `NULL` for any parameter, and `QueryOptions` represents such bindings as `null` `ByteBuffer` entries in the values list. Passing `null` to `Bytes.wrapForRead()` caused a `NullPointerException`. The fix adds an explicit null check: when the value is `null`, the string `"null"` is appended; otherwise the existing hex-dump logic runs.

### Before / After

```java
// BEFORE
Bytes bytes = Bytes.wrapForRead(value); // NPE when value == null
```

```java
// AFTER
if (null == value)
    sb.append("null").append(System.lineSeparator());
else {
    Bytes<ByteBuffer> bytes = Bytes.wrapForRead(value);
    // ... hex dump ...
}
```

---

## How to Spot

Any code that iterates over `QueryOptions.getValues()` and dereferences each element without a null check will NPE on queries with null-bound parameters. Null binding is a normal CQL feature (e.g., `INSERT INTO t (k, v) VALUES (?, ?)` with `null` for `v`), so every tool or handler that processes bound values must guard against null entries in the list.

---

## Test

A new `testDumpQueryNullValues` unit test in `DumpTest` passes a values list containing a non-null and a null `ByteBuffer`, invokes `Dump.dumpQuery`, and asserts that the line corresponding to the null value contains the string `"null"`.
