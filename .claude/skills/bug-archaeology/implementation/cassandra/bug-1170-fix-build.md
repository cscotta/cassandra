# Bug 1170: View.allKnownSSTables Missing, Breaking Ref Leak Detection

**Commit**: 3879bfd1eb · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/lifecycle/View.java
**Category**: Missing null / bounds check

---

## Root Cause

Ref-leak detection in `Ref.java` iterated over both `view.sstables` and `view.compacting` by directly accessing those fields and concatenating them with `Iterables.concat`. An SSTable can appear in `compacting` but not in `sstables` (it has been handed off for compaction and removed from the live set), so the raw union of the two sets could yield duplicates. More practically, the build broke because `view.compacting` is not a public set that callers should access directly; exposing the correct de-duplicated union required a dedicated helper. The fix introduces `allKnownSSTables()`, which uses `filterOut` to subtract already-listed SSTables from `compacting` before concatenating, giving an accurate and duplicate-free view for leak checking.

### Before / After

```java
// BEFORE: direct field access, possible duplicates
for (SSTableReader reader : Iterables.concat(view.sstables, view.compacting))
    reader.addTo(expected);
```

```java
// AFTER: encapsulated helper with deduplication
@VisibleForTesting
public Iterable<SSTableReader> allKnownSSTables() {
    return Iterables.concat(sstables, filterOut(compacting, sstables));
}
// ...
for (SSTableReader reader : view.allKnownSSTables())
    reader.addTo(expected);
```

---

## How to Spot

Directly accessing internal collection fields of another class to build a union is a red flag: the caller takes on the responsibility of knowing about deduplication invariants that the owning class should enforce. Any time two mutable sets that partially overlap are concatenated without a `filterOut` or `Sets.difference`, ask whether elements can appear in both and whether that is intentional.

---

## Test

No test added; the change is a build/compilation fix that restores the reference-leak checker to a compilable and correct state.
