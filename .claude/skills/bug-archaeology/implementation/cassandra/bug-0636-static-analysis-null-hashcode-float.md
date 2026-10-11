# Bug 0636: Multiple Static-Analysis Defects — Missing hashCode, Null Dereferences, Float Precision Loop

**Commit**: 1c3a2d247a · **Ticket**: CASSANDRA-13799
**Subsystem**: `cql3`, `io/compress`, `locator`, `index/sasi`, `tools/stress`
**Category**: Missing null / bounds check

---

## Root Cause

Several independent defects surfaced by lgtm.com static analysis:

1. `Expression.Bound` overrode `equals()` but not `hashCode()`, violating the Java contract and causing incorrect behavior in hash-based collections.
2. `FieldIdentifier.forUnquoted()` and `RoleName.setName()` called `String.toLowerCase()` without null-guarding, causing NPE if a null identifier was supplied.
3. `Distribution.average()` used a `float` loop counter (`d += 0.02f`), which accumulated floating-point error and could iterate 50 or 51 times non-deterministically; the fix rewrites to a `for (int count = 0; count < 51; ...)` loop.
4. `Booleans.generate()` used `% 1` (remainder after dividing by 1, always 0) instead of `% 2`, so it always returned `true`.
5. `SeedManager.managing.get(seed)` passed a `Seed` object where a `Long` key was expected, always returning null; the fix passes `seed.seed` (the long field).

### Before / After

```java
// BEFORE — Bound.hashCode missing; equals/hashCode contract violated
public boolean equals(Object other) { ... }
// no hashCode()
```

```java
// AFTER
public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(value);
    builder.append(inclusive);
    return builder.toHashCode();
}
```

```java
// BEFORE — always returns true
return identityDistribution.next() % 1 == 0;
// AFTER
return identityDistribution.next() % 2 == 0;
```

---

## How to Spot

Any class overriding `equals()` without a corresponding `hashCode()` override violates the Java language contract and will be flagged by most static analyzers. Float/double loop counters are a classic precision bug — counter-based loops should use integers. `% 1` is a mathematical no-op (always zero) and should be read as a likely typo for `% 2`.

---

## Test

`ExpressionTest` is added to verify that `Bound` objects behave correctly in sets and maps; existing `OperationFctsTest` and `InsertUpdateIfConditionTest` are updated to cover the corrected error messages.
