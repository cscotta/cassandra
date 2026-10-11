**Commit**: dfd996e51e · **Ticket**: KAFKA-18336

**Subsystem**: Benchmarks / JMH (ACL Authorizer)

**Category**: Wrong constant/default value

**Root cause**
The JMH benchmark for the ACL authorizer returned `void` from its `@Benchmark` methods, which allows the JIT to eliminate dead code and render the benchmark meaningless. Additionally, constants referenced inside the benchmark were declared as local variables (re-allocated each iteration) and the warmup iteration count was too low to reach a stable JIT-compiled state. The fixes return the result object, move constants to `@State` class fields, and increase the warmup iteration count.

**Before/After**
```java
// Before: void return, constant recreated per iteration
@Benchmark
public void testAclCheck() {
    ResourcePattern resource = new ResourcePattern(TOPIC, "test", LITERAL);
    authorizer.authorize(requestContext, List.of(new Action(READ, resource, 1, true, true)));
}

// After: result returned, constant hoisted to field
private ResourcePattern resource;

@Setup public void setup() {
    resource = new ResourcePattern(TOPIC, "test", LITERAL);
}

@Benchmark
public List<AuthorizationResult> testAclCheck() {
    return authorizer.authorize(requestContext, List.of(new Action(READ, resource, 1, true, true)));
}
```

**How to spot**
Any `@Benchmark` method with a `void` return type is a red flag unless a `Blackhole` parameter is used to consume results. Also check that expensive-to-construct objects used in the hot path are created in `@Setup`, not inside the benchmark method.

**Test**
The corrected benchmark is verified by running `./gradlew jmh` and observing non-trivial, stable throughput figures that change predictably with ACL list size.
