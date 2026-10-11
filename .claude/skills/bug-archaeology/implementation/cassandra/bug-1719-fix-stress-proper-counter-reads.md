# Bug 1719: Fix Stress to Do Proper Counter Reads

**Commit**: 3e2c610577 · **Ticket**: CASSANDRA-6835
**Subsystem**: tools/stress/operations (CqlCounterGetter, ThriftCounterGetter)
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

The stress tool's counter-read operations had two independent defects. First, `CqlCounterGetter` hardcoded the counter column family name as `"Counter1"` regardless of the command-line table setting, so reads always targeted the wrong table. Second, `ThriftCounterGetter` rejected runs when `variableColumnCount` was set and built its own `SliceRange` instead of using the shared `slicePredicate()` helper, drifting from the rest of the operation framework. The fix makes `CqlCounterGetter` use `state.type.table` and replaces the bespoke `ThriftCounterGetter` range logic with the common helper.

### Before / After

```java
// BEFORE (CqlCounterGetter)
String counterCF = state.isCql2() ? "Counter1" : "Counter3";
```

```java
// AFTER (CqlCounterGetter)
String counterCF = state.isCql2() ? state.type.table : "Counter3";
```

```java
// BEFORE (ThriftCounterGetter)
if (state.settings.columns.variableColumnCount)
    throw new IllegalStateException("Variable column counts not supported for counters");
SliceRange sliceRange = new SliceRange();
sliceRange.setStart(new byte[]{}).setFinish(new byte[]{});
sliceRange.setReversed(false).setCount(state.settings.columns.maxColumnsPerKey);
final SlicePredicate predicate = new SlicePredicate().setSlice_range(sliceRange);
```

```java
// AFTER (ThriftCounterGetter)
final SlicePredicate predicate = slicePredicate();
```

---

## How to Spot

Hardcoded table/column family names in operation classes are a strong signal that the code ignores user configuration. Always trace where names come from and verify they flow from the settings object rather than being literal strings.

---

## Test

No test added.
