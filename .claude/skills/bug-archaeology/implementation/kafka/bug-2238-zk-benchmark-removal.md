**Commit**: 07cafdd9df · **Ticket**: KAFKA-17729
**Subsystem**: jmh-benchmarks (AuthorizerBenchmark)
**Category**: State not cleaned up

**Root cause**
`AuthorizerBenchmark` included a benchmark variant for `AclAuthorizer` (ZooKeeper-based authorizer) and `MetadataRequestBenchmark` existed as a separate file. Both are dead code in a KRaft-only world: `AclAuthorizer` requires ZooKeeper, which is removed. Keeping ZK-based benchmarks inflated compilation time, could mislead future contributors into thinking ZK paths were still tested, and required ZooKeeper mock infrastructure that was difficult to maintain.

**Before/After**
```java
// Before – ZK benchmark variant present
@Benchmark
public void benchmarkAclAuthorizer(Blackhole bh) { ... }

// After – only StandardAuthorizer (KRaft) variant retained
@Benchmark
public void benchmarkStandardAuthorizer(Blackhole bh) { ... }
```

**How to spot**
After removing a subsystem (ZooKeeper, EOS-v1, etc.), search for benchmark, test, and example files that import or instantiate classes from that subsystem; they are dead code and should be removed to avoid false confidence in test coverage.

**Test**
No test added.
