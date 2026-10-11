# Bug 1475: Wrong Type for Ratios in SampledOpDistributionFactory After Merge

**Commit**: a44d641e8f · **Ticket**: CASSANDRA-7519
**Subsystem**: tools/stress/src/org/apache/cassandra/stress/operations/SampledOpDistributionFactory.java
**Category**: Logic error in condition

---

## Root Cause

A merge of CASSANDRA-7519 into the stress tool left `SampledOpDistributionFactory` with an incorrect type for the `ratios` field: it was declared as `List<Pair<T, Double>>` but callers were passing a `Map<T, Double>`. Every method that iterated over `ratios` used `Pair` accessor methods (`getFirst()`, `getSecond()`) instead of `Map.Entry` methods (`getKey()`, `getValue()`), meaning the code would not compile or would produce incorrect iteration behavior. The fix changes the field type to `Map<T, Double>` and updates all iteration sites accordingly.

### Before / After

```java
// BEFORE
final List<Pair<T, Double>> ratios;

protected SampledOpDistributionFactory(List<Pair<T, Double>> ratios, DistributionFactory clustering)

for (Pair<T, Double> ratio : ratios)
    operations.add(new Pair<>(get(timer, generator, ratio.getFirst()), ratio.getSecond()));
```

```java
// AFTER
final Map<T, Double> ratios;

protected SampledOpDistributionFactory(Map<T, Double> ratios, DistributionFactory clustering)

for (Map.Entry<T, Double> ratio : ratios.entrySet())
    operations.add(new Pair<>(get(timer, generator, ratio.getKey()), ratio.getValue()));
```

---

## How to Spot

When merging changes that refactor a field's type, verify that all use sites (constructors, loops, accessors) are updated to match the new type. A compile-time check would catch this; missing the field declaration update while updating callers is a classic partial-merge mistake.

---

## Test

No test added.
