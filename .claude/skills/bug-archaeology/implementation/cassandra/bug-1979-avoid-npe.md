# Bug 1979: NPE in ModificationStatement when conditions list is null

**Commit**: ed645f32aa · **Ticket**: n/a
**Subsystem**: cql3/statements/ModificationStatement.java
**Category**: Missing null / bounds check

---

## Root Cause

The `ModificationStatement` constructor assigned `this.conditions = conditions` directly. When callers passed `null` for the conditions list (valid when no IF clause is present), any subsequent iteration over `this.conditions` threw a `NullPointerException`. The fix normalises `null` to an empty list at construction time.

### Before / After

```java
// BEFORE: null conditions stored as-is
this.conditions = conditions;
```

```java
// AFTER: null normalised to empty list
this.conditions = conditions == null
    ? Collections.<Pair<ColumnIdentifier, Operation.RawUpdate>>emptyList()
    : conditions;
```

---

## How to Spot

Whenever a collection field is set from a constructor parameter that can legitimately be `null`, normalise it to an empty collection immediately rather than relying on callers to always supply a non-null value.

---

## Test

No test added.
