**Commit**: 7005a1fa4b · **Ticket**: KAFKA-17433
**Subsystem**: .github/workflows
**Category**: Missing null / bounds check

**Root cause**
Flaky tests in Kafka's CI required developers to manually re-run the entire CI suite to investigate whether a single test was deterministically failing or just intermittently flaky. There was no mechanism to repeat a specific test N times to gather statistical evidence. The design gap was that `ClusterTestExtensions` had no concept of a repeat count, making deflaking require expensive full-suite reruns.

**Before/After**
```java
// Before - no repeat support
// Process single @ClusterTest annotation
generatedContexts.addAll(processClusterTest(context, clusterTestAnnot, defaults));

// After - reads system property for repeat count
int repeatCount = getTestRepeatCount(); // reads kafka.cluster.test.repeat
List<TestTemplateInvocationContext> ret = IntStream.range(0, repeatCount)
    .mapToObj(__ -> Arrays.stream(clusterTests))
    .flatMap(...)
    .collect(Collectors.toList());
```

**How to spot**
When a test framework extension generates `TestTemplateInvocationContext` objects, check whether it can multiply the context list for repeatability. Missing repeat support means CI engineers resort to re-running whole modules rather than targeted deflaking, wasting build time.

**Test**
`deflake.yml` GitHub Actions workflow added; `ClusterTestExtensions` extended with `kafka.cluster.test.repeat` property.
