# Bug 0883: Unnecessary boxing in StreamingHistogram TreeMap comparator

**Commit**: f1cabcade8 · **Ticket**: n/a
**Subsystem**: Utilities / StreamingHistogram
**Category**: Wrong constant / default value

---

## Root Cause
The `TreeMap` comparator in `StreamingHistogram` called `((Double) o1.doubleValue()).compareTo(o2.doubleValue())`, which boxes the primitive `double` returned by `doubleValue()` into a `Double` object on every comparison. This is both an unnecessary allocation and a minor performance hazard in a hot path. The fix replaces it with `Double.compare(o1.doubleValue(), o2.doubleValue())`, which avoids boxing entirely.

### Before / After
```java
// BEFORE — boxes primitive double into Double on every comparison
return ((Double)o1.doubleValue()).compareTo(o2.doubleValue());

// AFTER — no boxing
return Double.compare(o1.doubleValue(), o2.doubleValue());
```

---

## How to Spot
In any `Comparator` that works with numeric types, look for explicit casts to boxed types (`Integer`, `Double`, `Long`) before calling `compareTo`. The idiomatic replacements are `Integer.compare()`, `Double.compare()`, and `Long.compare()`. Static analysis tools (FindBugs/SpotBugs rule `BX_BOXING_IMMEDIATELY_UNBOXED_TO_PERFORM_COERCION`) catch this pattern.

---

## Test
No test added.
