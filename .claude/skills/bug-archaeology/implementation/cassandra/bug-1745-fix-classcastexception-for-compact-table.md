# Bug 1745: Fix ClassCastException for Compact Table with Composite Partition Key

**Commit**: 15728ad8df · **Ticket**: CASSANDRA-6738
**Subsystem**: src/java/org/apache/cassandra/db/composites/
**Category**: Missing null / bounds check

---

## Root Cause

For backward-compatibility reasons, composite dense tables (compact storage tables with a composite partition key) can produce cell names whose component count is smaller than the full clustering-column count. `CompoundComposite.get(int i)` performed no bounds check and would return `elements[i]` even when `i >= size()`, causing an `ArrayIndexOutOfBoundsException` or a `ClassCastException` downstream when callers treated the raw array slot as a valid `ByteBuffer`. The fix returns `null` for out-of-bounds indices so callers that already handle `null` gracefully continue to work, and a new package-private constructor for `CompoundDenseCellName` was added so that the cell name can carry a `size` smaller than `components.length`.

### Before / After

```java
// BEFORE (CompoundComposite.get)
public ByteBuffer get(int i)
{
    return elements[i];
}
```

```java
// AFTER (CompoundComposite.get)
public ByteBuffer get(int i)
{
    return i >= size() ? null : elements[i];
}
```

---

## How to Spot

Array accesses on composite/multi-component objects are dangerous when the logical size of the object can differ from the backing-array length. During review, check every `array[index]` access in composite-type classes to confirm that `index` is validated against the logical size, not just the raw array length.

---

## Test

No test added.
