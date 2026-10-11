**Commit**: 6f1a6ae868 · **Ticket**: n/a (MINOR)

**Subsystem**: coordinator-common/src/test/java/org/apache/kafka/coordinator/common/runtime/CoordinatorRuntimeMetricsImplTest.java

**Category**: Logic error in condition

**Root cause**
The coordinator runtime metrics isolation tests were written as one test per metric, requiring a developer to manually remember to add a new test for every new metric. This approach was error-prone: newly introduced metrics would silently lack isolation tests. The single-test-per-metric pattern was replaced with a single parametric test that automatically harvests all sensor and metric names from two independently constructed `CoordinatorRuntimeMetricsImpl` instances and asserts no overlap, catching isolation regressions for any future metric automatically.

**Before**
```java
// Before: separate isolation test for each metric
@Test
public void testGroupCoordinatorMetricsDoNotLeakIntoShareCoordinator() {
    // manually checked one metric at a time
    assertNotEquals(metric1.name(), shareMetric1.name());
}
```

**After**
```java
// After: single generic isolation test
@Test
public void testMetricsIsolation() {
    Set<MetricName> names1 = captureRegisteredMetrics(instance1);
    Set<MetricName> names2 = captureRegisteredMetrics(instance2);
    assertTrue(Collections.disjoint(names1, names2));
}
```

**How to spot**
When you see a cluster of near-identical test methods in a metrics test class that each check one metric name individually, the test suite is likely incomplete; a missing entry for a new metric will go undetected. Look for patterns where adding a metric requires manually updating multiple test methods.

**Test**
Add a new metric to `CoordinatorRuntimeMetricsImpl` without updating the test and verify the updated generic test catches the overlap immediately.
