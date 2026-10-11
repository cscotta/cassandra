# Bug 1770: Fix CFMetaData.equals()

**Commit**: 0ced33c6ee · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/config/CFMetaData.java
**Category**: Logic error in condition

---

## Root Cause

The `equals()` method in `CFMetaData` contained a copy-paste error: two field comparisons used `Objects.equal(...)` (i.e., equality is `true`) as the condition for returning `false`. This inverted the semantics — the method returned `false` when `droppedColumns` and `triggers` were **equal**, and continued (returning `true`) when they **differed**. The result was that two `CFMetaData` instances with different dropped columns or triggers were incorrectly considered equal.

### Before / After

```java
// BEFORE – missing negation makes equality checks inverted
if (Objects.equal(droppedColumns, rhs.droppedColumns)) return false;
if (Objects.equal(triggers, rhs.triggers)) return false;
```

```java
// AFTER – correct negation mirrors every other field comparison
if (!Objects.equal(droppedColumns, rhs.droppedColumns)) return false;
if (!Objects.equal(triggers, rhs.triggers)) return false;
```

---

## How to Spot

In hand-written `equals()` methods, every field comparison must use the same polarity. Reviewers should scan for mixed uses of `if (equal(...)) return false` and `if (!equal(...)) return false` within the same method; any inconsistency is almost certainly a copy-paste bug.

---

## Test

No test added.
