# Bug 1320: Merge-Broken Method Signatures and Field Visibility in IndexSummary and OHCProvider

**Commit**: 034e0179b8 · **Ticket**: n/a
**Subsystem**: io/sstable/IndexSummary, io/sstable/IndexSummaryBuilder, cache/OHCProvider
**Category**: Missing override of default method

---

## Root Cause

A bad merge left three related classes in an inconsistent state: `OHCProvider`'s stub `write` method had the wrong signature (missing `offset` and `length` parameters), `IndexSummary.getOffHeapSize()` was package-private when callers in other packages required it to be public, and the fields of `IndexSummaryBuilder.ReadableBoundary` were package-private when cross-package access was needed. These are all the same class of merge error: the merged result adopted the wrong access modifier or signature from one branch rather than the other.

### Before / After

```java
// BEFORE
public void write(Memory memory) throws IOException { ... }

long getOffHeapSize() { ... }

final DecoratedKey lastKey;
final long indexLength;
```

```java
// AFTER
public void write(Memory memory, long offset, long length) throws IOException { ... }

public long getOffHeapSize() { ... }

public final DecoratedKey lastKey;
public final long indexLength;
```

---

## How to Spot

After any merge that spans an interface or abstract class, verify that every concrete implementation still matches the current interface contract — method signatures, parameter lists, and return types must agree exactly. Also audit visibility: if a field or method was made public in one branch to support cross-package access, a merge that silently reverts it to package-private will compile fine within the same package but break callers elsewhere. Running a full build immediately after a merge is the cheapest detector.

---

## Test

No test added.
