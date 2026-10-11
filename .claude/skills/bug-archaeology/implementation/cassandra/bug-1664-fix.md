# Bug 1664: Fix CFMetaData#getColumnDefinitionFromColumnName()

**Commit**: c3550fe40c · **Ticket**: CASSANDRA-7047
**Subsystem**: src/java/org/apache/cassandra/config/CFMetaData.java
**Category**: Silent skip on non-match

---

## Root Cause

In `CFMetaData#getColumnDefinitionFromColumnName()`, the loop over column definitions used `break` when `def.componentIndex` exceeded the available composite name components. This caused the entire search to terminate early rather than moving on to the next candidate definition. Any column definition that appeared later in the iteration order would never be checked.

### Before / After

```java
// BEFORE
if (def.componentIndex >= components.length)
    break;
```

```java
// AFTER
if (def.componentIndex >= components.length)
    continue;
```

---

## How to Spot

When iterating over a collection to find a matching element, using `break` on a non-match condition is almost always wrong — it abandons the search instead of moving to the next candidate. Any loop of the form "for each X, if some precondition fails → break" should be scrutinised for whether `continue` was intended.

---

## Test

No test added.
