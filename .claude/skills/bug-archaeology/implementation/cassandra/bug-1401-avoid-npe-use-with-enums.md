# Bug 1401: avoid npe chance, use == with enums

**Commit**: 2c3f9ba048 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/cql3/ColumnCondition.java
**Category**: Missing null / bounds check

---

## Root Cause

Throughout the codebase, enum comparisons were written as `enumVar.equals(SomeEnum.VALUE)`. While this is semantically correct for non-null references, calling `.equals()` on a potentially-null variable introduces an NPE risk that `==` avoids entirely. Java guarantees enum constants are singletons, so `==` is both safe and idiomatic. The widespread use of `.equals()` for enum comparisons was a latent hazard: any code path that allowed a null enum field (e.g. an uninitialized `operator` or `level` or `operationMode`) would produce an NPE at the comparison site rather than being caught earlier or failing with a clearer error.

### Before / After

```java
// BEFORE
if (!operator.equals(Operator.IN))
    assert this.inValues == null;

if (level.equals(Level.ROOT))
    return true;

if (operationMode.equals(Mode.JOINING))
    throw new IOException("Cannot snapshot until bootstrap completes");
```

```java
// AFTER
if (operator != Operator.IN)
    assert this.inValues == null;

if (level == Level.ROOT)
    return true;

if (operationMode == Mode.JOINING)
    throw new IOException("Cannot snapshot until bootstrap completes");
```

---

## How to Spot

Search the codebase for `.equals(` calls where both sides are enum types. IDEs and static analysis tools (e.g. FindBugs/SpotBugs rule `EC_BAD_ARRAY_COMPARE`, PMD `UseEqualsToCompareEnums`) flag this pattern automatically. In code review, any `someEnumVar.equals(SomeEnum.CONSTANT)` should be replaced with `someEnumVar == SomeEnum.CONSTANT` — especially on variables that could conceivably be null through construction, deserialization, or reflection.

---

## Test

No test added.
