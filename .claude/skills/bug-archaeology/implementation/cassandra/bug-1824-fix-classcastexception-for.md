# Bug 1824: ClassCastException in CleanupInfo Due to Hardcoded SSTableScanner Type

**Commit**: 5d24c55335 · **Ticket**: CASSANDRA-6462
**Subsystem**: src/java/org/apache/cassandra/db/compaction
**Category**: Logic error in condition

---

## Root Cause

`CleanupInfo` stored the scanner as the concrete `SSTableScanner` type rather than the `ICompactionScanner` interface. When `cleanupStrategy.getScanner()` returned a different implementation (e.g., a wrapped or decorated scanner), the explicit cast `(SSTableScanner)scanner` at the call site threw a `ClassCastException`. The fix widens the field and constructor parameter to the interface type, eliminating the unsafe downcast.

### Before / After

```java
// BEFORE
private final SSTableScanner scanner;

public CleanupInfo(SSTableReader sstable, SSTableScanner scanner) { ... }

CleanupInfo ci = new CleanupInfo(sstable, (SSTableScanner)scanner);
```

```java
// AFTER
private final ICompactionScanner scanner;

public CleanupInfo(SSTableReader sstable, ICompactionScanner scanner) { ... }

CleanupInfo ci = new CleanupInfo(sstable, scanner);
```

---

## How to Spot

When a factory/strategy method is declared to return an interface type, storing or casting the result to a concrete subtype is a code smell — any future implementation change will produce a `ClassCastException` at runtime rather than a compile-time error. Always use the declared interface type.

---

## Test

No test added.
