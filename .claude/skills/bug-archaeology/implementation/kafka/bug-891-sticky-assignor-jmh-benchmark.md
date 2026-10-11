**Commit**: dabde76ebf · **Ticket**: KAFKA-19477

**Subsystem**: JMH Benchmarks / StickyTaskAssignor

**Category**: Missing test coverage

**Root cause**
`StickyTaskAssignor` had no JMH benchmark, making it impossible to quantify the performance impact of algorithmic changes. Without a benchmark, the removal of `TaskPairs` and future assignor changes could not be validated for throughput regression. The new benchmark accepts configurable parameters for member count, subtopology count, partition count, and standby replica count, enabling systematic performance characterization across realistic topology sizes.

**Before/After**
```java
// Before: no JMH benchmark for StickyTaskAssignor

// After: new parameterized benchmark class
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
public class StickyTaskAssignorBenchmark {
    @Param({"10", "100", "1000"}) int memberCount;
    @Param({"5", "50"})           int subtopologyCount;

    @Benchmark
    public AssignmentResult assign(Blackhole bh) {
        return assignor.assign(members, tasks);
    }
}
```

**How to spot**
Core algorithmic components (assignors, rebalancers, partitioners) that lack JMH benchmarks cannot have their performance validated across changes. Any class accepting configurable "count" parameters as input to an O(n log n) or worse algorithm is a good benchmark candidate.

**Test**
`StickyTaskAssignorBenchmark` runs via `./gradlew jmh` and produces stable average-time measurements used to validate that post-`TaskPairs` assignment performance is equal to or better than the prior implementation.
