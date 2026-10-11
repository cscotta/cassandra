# Bug 1383: ArrayIndexOutOfBoundsException When Building CQL Syntax Error Message

**Commit**: 254d6f7e9c · **Ticket**: CASSANDRA-8455
**Subsystem**: src/java/org/apache/cassandra/cql3/ErrorCollector.java
**Category**: Missing null / bounds check

---

## Root Cause

`ErrorCollector.appendSnippet()` received ANTLR `Token` objects describing the source range of a syntax error and used `token.getLine()` and `token.getCharPositionInLine()` to index into the query's split lines. For certain malformed queries (e.g., a missing closing bracket at the end of a statement), ANTLR produces sentinel tokens with `line == 0` and `charPositionInLine == -1`, representing "no position". The code blindly used these values as array indices, causing `lines[0 - 1]` or similar out-of-bounds access, throwing `ArrayIndexOutOfBoundsException` instead of producing a useful error message.

### Before / After

```java
// BEFORE: no guard; invalid token positions used directly as array indices
String[] lines = query.split("\n");
boolean includeQueryStart = (from.getLine() == 1) && (from.getCharPositionInLine() == 0);
// ... proceeds to index 'lines' using from.getLine() - 1, which is -1 for sentinel tokens
```

```java
// AFTER: sentinel tokens are detected and the snippet is silently skipped
if (!areTokensValid(from, to, offending))
    return;

String[] lines = query.split("\n");
// ... safe to proceed
```

---

## How to Spot

Any time external input (parser output, serialized data, network-provided offsets) is used to index an array or string, the code must validate that the value falls within `[0, length)` before the access. ANTLR sentinel tokens with zero-based or negative positions are a well-known edge case for error-recovery paths; a reviewer should check that all `getLine()` / `getCharPositionInLine()` usages are guarded, particularly in error-handling code where the token may represent "no token".

---

## Test

`ErrorCollectorTest.testAppendSnippetWithInvalidToToken()` was added, passing a mock token with `line=0` and `charPositionInLine=-1` to verify that `appendSnippet` returns an empty string rather than throwing.
