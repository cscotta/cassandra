# Bug 0144: UNSET Collection Type Crashes Query Tracing

**Commit**: c53d3ac8c6 · **Ticket**: CASSANDRA-19880
**Subsystem**: transport/messages (ExecuteMessage)
**Category**: Missing null / bounds check

---

## Root Cause

When query tracing is enabled for a prepared statement execution, `ExecuteMessage` iterates over all bound values and calls `cs.type.asCQL3Type().toCQLLiteral(bytes, ...)` to render each one. The `UNSET_BYTE_BUFFER` sentinel value (used when a bind variable is intentionally left unset) is a special marker that is not a valid CQL value; passing it to `toCQLLiteral()` causes a crash or garbled output because the type's serializer does not handle the unset sentinel. The fix checks for the sentinel before dispatching to the type serializer and renders it as the literal string `<unset>`.

### Before / After

```java
// BEFORE
String boundValue = cs.type.asCQL3Type().toCQLLiteral(options.getValues().get(i), options.getProtocolVersion());
```

```java
// AFTER
ByteBuffer bytes = options.getValues().get(i);
String boundValue = (bytes == ByteBufferUtil.UNSET_BYTE_BUFFER) ? "<unset>" : cs.type.asCQL3Type().toCQLLiteral(bytes, options.getProtocolVersion());
```

---

## How to Spot

Whenever tracing code iterates bound values generically, it must handle the full range of sentinel values that the bind-value list can hold — not just `null` but also `UNSET_BYTE_BUFFER`. Any site that calls a type serializer on a `ByteBuffer` obtained from user-supplied bind values without first checking for the unset sentinel is at risk of a crash or incorrect output when clients explicitly unset a variable.

---

## Test

Executes a prepared INSERT with one and then two bind variables marked unset via `BoundStatement.unset()` and asserts that the query trace renders those slots as `<unset>`.
