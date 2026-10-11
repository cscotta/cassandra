# Bug 0116: SAI Indexes Empty Values for Non-Literal Types That Forbid Them

**Commit**: f28a16740a · **Ticket**: CASSANDRA-20313
**Subsystem**: `src/java/org/apache/cassandra/index/sai`
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

The SAI index writer (both SSTable-level and Memtable-level) guarded against indexing empty byte buffers with the condition `!indexType.allowsEmpty()`. This was correct for literal (text/blob) types that allow empty values as valid data, but it missed the case of non-literal types: even when a type permits an empty buffer as a raw encoding, a non-literal SAI index cannot meaningfully encode or query that empty value, so indexing it causes downstream failures. The fix introduces `IndexTermType.skipsEmptyValue()` which returns `true` when either the type does not allow empty values OR the index is non-literal, and uses that predicate at both write paths.

### Before / After

```java
// BEFORE – only checks allowsEmpty(), ignores isLiteral()
if (term.remaining() == 0 && !index.termType().indexType().allowsEmpty()) return;
```

```java
// AFTER – skips empty if type disallows it OR if the index is non-literal
if (term.remaining() == 0 && index.termType().skipsEmptyValue()) return;

// New helper:
public boolean skipsEmptyValue() {
    return !indexType.allowsEmpty() || !isLiteral();
}
```

---

## How to Spot

Whenever a guard condition combines two independent safety requirements with `AND` (meaning both must be true to skip), consider whether the correct logic is `OR` (either is sufficient to skip). Here the original `!allowsEmpty()` check was a subset of the full requirement: non-literal types need to skip empty values even when the underlying Java type technically "allows" an empty buffer. During review, verify that empty-value handling covers all axes of variation (type capability AND index kind).

---

## Test

`EmptyValuesTest#testEmptyValues` uses a property-based generator over all primitive CQL types to verify that empty-value inserts are retrievable via `WHERE v = ?` (empty) only for types and index kinds that actually index them.
