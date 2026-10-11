**Commit**: 3404f65cdb · **Ticket**: KAFKA-19324
**Subsystem**: checkstyle/import-control-transaction-coordinator.xml
**Category**: Logic error in condition

**Root cause**
Description

* Replace `org.apache.kafka.common.test.TestUtils` with
`org.apache.kafka.test.TestUtils` in outer package modules to
standardize test utility usage
* Move `waitUntilLeaderIsElectedOrChangedWithAdmin` method from
`org.apache.kafka.test.TestUtils` to `ClusterInstance` and refactor for
better code organization
* Add `org.apache.kafka.test.TestUtils` dependency to
`transaction-coordinator` import control

Reviewers: PoAn Yang [payang@apache.org](mailto:payang@apache.org), Ken
 Huang  [s7133700@gmail.com](mailto:s7133700@gmail.com), Ken Huang
 [s7133700@gmail.com](mailto:s7133700@gmail.com), Chia-Ping Tsai
 [chia7712@gmail.com](mailto:chia7712@gmail.com)

**Before/After**
```java
// Before
import org.apache.kafka.common.test.TestUtils;
import org.apache.kafka.common.test.TestUtils;
import org.apache.kafka.common.test.TestUtils;
import org.apache.kafka.common.test.TestUtils;

// After
            <allow pkg="org.apache.kafka.test" />
import org.apache.kafka.test.TestUtils;
import org.apache.kafka.test.TestUtils;
import org.apache.kafka.test.TestUtils;
import org.apache.kafka.test.TestUtils;
```

**How to spot**
When reviewing checkstyle/import-control-transaction-coordinator.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19324 make org.apache.kafka.common.test.testutils package-private to prevent cross-module access (#19884).

**Test**
Test modified in ClientRebootstrapTest.java covering the corrected behaviour.
