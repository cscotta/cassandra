# Bug 1384: Avoid stack overflow on large clustering IN values

**Commit**: 9dc9185f5c · **Ticket**: CASSANDRA-8410
**Subsystem**: db/filter/ColumnSlice.java
**Category**: Logic error in condition

---

## Root Cause

`ColumnSlice.SpecificNamesIterator.computeNext()` used tail recursion to advance to the next slice when a slice was exhausted: after nulling out `currentSlice` it immediately called `return computeNext()`. With a large IN clause each value maps to a separate slice, so for a query like `WHERE ck IN (v1, v2, ..., vN)` the iterator recurses once per value. With thousands of values the JVM stack depth is exceeded, producing a `StackOverflowError`.

The pattern is a classical conversion failure: an algorithm written with recursion that is safe for small inputs but has no tail-call optimization in Java.

### Before / After

```java
// BEFORE
if (currentSlice.hasNext())
    return currentSlice.next();

currentSlice = null;
return computeNext();   // recursive call — blows the stack for large IN lists
```

```java
// AFTER
while (currentSlice != null || idx < slices.length)
{
    if (currentSlice == null)
    {
        ColumnSlice slice = slices[idx++];
        // ... set up currentSlice ...
    }
    if (currentSlice.hasNext())
        return currentSlice.next();
    currentSlice = null;
}
return endOfData();
```

---

## How to Spot

Any `computeNext()` / `hasNext()` / `advance()` implementation that calls itself recursively to move to the next bucket or slice is at risk on large inputs. Review iterator patterns for self-calls and replace with an iterative `while` loop that drives all slice transitions.

---

## Test

No test added.
