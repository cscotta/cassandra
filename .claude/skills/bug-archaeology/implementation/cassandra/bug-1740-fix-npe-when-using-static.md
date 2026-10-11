# Bug 1740: Fix NPE When Using Static Columns and IN

**Commit**: fd609339ee · **Ticket**: CASSANDRA-6769
**Subsystem**: src/java/org/apache/cassandra/cql3/statements
**Category**: Off-by-one

---

## Root Cause

In `SelectStatement`, when building the `ColumnSlice` array for a query that combines a static column slice with a list of regular column slices, the array was allocated as `l.size() + 1` to hold the static slice at index 0 followed by the regular slices. However, the loop that populated regular slices used `slices[i]` instead of `slices[i+1]`, overwriting the static slice entry at index 0 and leaving the last slot null. A subsequent null dereference on the uninitialized last slot produced the NPE.

### Before / After

```java
// BEFORE
slices = new ColumnSlice[l.size()+1];
slices[0] = staticSlice;
for (int i = 0; i < l.size(); i++)
    slices[i] = l.get(i);   // overwrites slices[0], leaves slices[l.size()] null
```

```java
// AFTER
slices = new ColumnSlice[l.size()+1];
slices[0] = staticSlice;
for (int i = 0; i < l.size(); i++)
    slices[i+1] = l.get(i); // correctly fills indices 1..l.size()
```

---

## How to Spot

Whenever an array is pre-filled at index 0 before a loop that iterates with `slices[i]`, check whether `i` should be offset by the number of pre-filled entries. A common sign is that the array size is `list.size() + N` but the loop starts at `i = 0` without an offset.

---

## Test

No test added.
