**Commit**: 9292a22606 · **Ticket**: KAFKA-19049
**Subsystem**: clients/clients-integration-tests
**Category**: Logic error in condition

**Root cause**
jira: https://issues.apache.org/jira/browse/KAFKA-19049

[KAFKA-18617](https://issues.apache.org/jira/browse/KAFKA-18617)
introduced the mechanism to inject the cluster test at runtime, so the
integration tests don't need to use
`@ExtendWith(ClusterTestExtensions.class)` any more. Reviewers: PoAn Yang <payang@apache.org>, Chia-Ping Tsai
<chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.test.junit.ClusterTestExtensions;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ClusterTestExtensions.class)
import org.apache.kafka.common.test.junit.ClusterTestExtensions;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(value = ClusterTestExtensions.class)
import org.apache.kafka.common.test.junit.ClusterTestExtensions;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ClusterTestExtensions.class)

// After
// (no additions)
```

**How to spot**
When reviewing clients/clients-integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19049 remove the `@extendwith(clustertestextensions.class)` from code base (#19299).

**Test**
Test modified in DescribeAuthorizedOperationsTest.java covering the corrected behaviour.
