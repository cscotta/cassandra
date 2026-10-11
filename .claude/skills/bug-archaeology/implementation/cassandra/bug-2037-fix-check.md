# Bug 2037: SSTableNamesIterator Reads One Extra Column Due to Off-by-One in Break Condition

**Commit**: 79c2ff7c7e · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/columniterator/SSTableNamesIterator.java
**Category**: Off-by-one

---

## Root Cause

`SSTableNamesIterator` scans SSTable columns looking for a set of requested names, breaking early once all have been found. The break condition used `> columns.size()` (strict greater-than), so the iterator continued reading one column past the last requested match before stopping. The fix changes the condition to `>= columns.size()` so the loop exits as soon as the count of matched columns equals the number of columns requested.

### Before / After

```java
// BEFORE
if (++n > columns.size())
    break;
```

```java
// AFTER
if (++n >= columns.size())
    break;
```

---

## How to Spot

Early-exit conditions on counters that track "how many items have been found so far" should use `>=` (found at least as many as needed) rather than `>` (found strictly more than needed). Using `>` causes one unnecessary extra iteration.

---

## Test

No test added.
