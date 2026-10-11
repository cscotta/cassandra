# Bug 1705: Fix off-by-one error in stress

**Commit**: 53e22120d0 · **Ticket**: CASSANDRA-6883
**Subsystem**: tools/stress/settings/OptionDistribution
**Category**: Off-by-one

---

## Root Cause

The `UniformRealDistribution` upper bound is exclusive, so constructing it with `(min, max)` means the distribution never actually produces the value `max`. The fix passes `max + 1` as the upper bound so that the full closed interval `[min, max]` is covered when the distribution is sampled and rounded back to an integer.

### Before / After

```java
// BEFORE
return new DistributionBoundApache(new UniformRealDistribution(min, max), min, max);
```

```java
// AFTER
return new DistributionBoundApache(new UniformRealDistribution(min, max + 1), min, max);
```

---

## How to Spot

Whenever wrapping a half-open `[a, b)` distribution API to represent a closed `[a, b]` range, confirm the upper bound is shifted by one (or an epsilon). Missing this shift silently excludes the maximum value from the sample space.

---

## Test

No test added.
