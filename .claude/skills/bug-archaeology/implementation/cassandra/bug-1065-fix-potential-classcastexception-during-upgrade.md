# Bug 1065: ClassCastException in BTreeRow During Upgrade

**Commit**: 26c8892859 · **Ticket**: CASSANDRA-10468
**Subsystem**: src/java/org/apache/cassandra/db/rows/BTreeRow.java
**Category**: Wrong constant / default value

---

## Root Cause

`BTreeRow` maintains two separate iterators — one for simple columns (`simpleIdx`) and one for complex columns (`complexIdx`). The method `getComplexIdx()` was supposed to compute the position of the current complex-column entry in the backing array but accidentally used `simpleIdx` instead of `complexIdx`. During upgrade, when rows can contain both simple and complex columns and traversal order is reversed, this caused the wrong BTree index to be returned, leading to a `ClassCastException` when the retrieved object was cast to the complex-column type.

### Before / After

```java
// BEFORE — uses simpleIdx instead of complexIdx
private int getComplexIdx()
{
    return reversed ? data.length - simpleIdx - 1 : simpleIdx;
}
```

```java
// AFTER — uses the correct field
private int getComplexIdx()
{
    return reversed ? data.length - complexIdx - 1 : complexIdx;
}
```

---

## How to Spot

When a class has parallel tracking variables for conceptually different index sequences (here `simpleIdx` vs `complexIdx`), review every helper method that computes a position to confirm it references the intended variable. Copy-paste mistakes like this are especially easy to miss in symmetrical reversed/forward expressions where both variables have the same type and similar names. A code review heuristic: any method whose name contains a noun (`Complex`, `Simple`) should reference only the variable associated with that noun.

---

## Test

No test added.
