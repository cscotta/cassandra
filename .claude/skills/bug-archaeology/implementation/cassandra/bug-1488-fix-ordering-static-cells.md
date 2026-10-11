# Bug 1488: Fix Ordering of Static Cells

**Commit**: 56db00180f · **Ticket**: CASSANDRA-7763
**Subsystem**: src/java/org/apache/cassandra/db/composites
**Category**: Logic error in condition

---

## Root Cause

The `compare()` method in `AbstractCType` and `CompoundSparseCellNameType.WithCollection` fell through to byte-by-byte comparator logic even when one composite was static and the other was not. Static cells have a special marker byte in their composite encoding, but that byte could sort them anywhere relative to regular cells depending on its value. The fix adds an early-exit guard: if one composite is static and the other is not, static sorts before non-static unconditionally, with empty always sorting first regardless.

### Before / After

```java
// BEFORE
public int compare(Composite c1, Composite c2)
{
    int s1 = c1.size();
    int s2 = c2.size();
    // falls through to element-by-element comparison even when
    // one is static and the other is not
```

```java
// AFTER
public int compare(Composite c1, Composite c2)
{
    if (c1.isStatic() != c2.isStatic())
    {
        // Static sorts before non-static no matter what, except for empty which
        // always sort first
        if (c1.isEmpty())
            return c2.isEmpty() ? 0 : -1;
        if (c2.isEmpty())
            return 1;
        return c1.isStatic() ? -1 : 1;
    }

    int s1 = c1.size();
    int s2 = c2.size();
```

---

## How to Spot

When a comparator handles objects with distinct "kinds" (static vs. regular, tombstone vs. live, etc.), check that the compare/equals logic short-circuits on kind mismatch before doing field-by-field comparison. Missing that guard lets encoding artifacts produce incorrect ordering.

---

## Test

No test added.
