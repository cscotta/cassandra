# Bug 1515: Avoid IOOBE when building SyntaxError message snippet

**Commit**: 72241f6d72 · **Ticket**: CASSANDRA-7569
**Subsystem**: src/java/org/apache/cassandra/cql3
**Category**: Missing null / bounds check

---

## Root Cause

`ErrorCollector.appendSnippet()` retrieved the offending token from the `TokenStream` using the raw index reported by ANTLR. When the error occurs at an extra character appended after the last valid token (e.g., a trailing character), ANTLR reports an index equal to the stream size, which is one past the end of the valid index range. Calling `tokenStream.get(index)` with that out-of-bounds index throws an `IndexOutOfBoundsException` instead of producing the error message. The fix clamps the index to `size - 1` via a new helper `getOffendingTokenIndex()`.

### Before / After

```java
// BEFORE
Token offending = tokenStream.get(index);
```

```java
// AFTER
Token offending = tokenStream.get(getOffendingTokenIndex(index, size));

// helper:
private static int getOffendingTokenIndex(int index, int size)
{
    return Math.min(index, size - 1);
}
```

---

## How to Spot

Whenever an index comes from an external source (parser, user input, external library) and is used directly as an array or list index, verify it is clamped or validated against the collection's bounds. Off-by-one conditions at the upper boundary are especially common when an index is allowed to equal `size` as a sentinel value for EOF.

---

## Test

No test added.
