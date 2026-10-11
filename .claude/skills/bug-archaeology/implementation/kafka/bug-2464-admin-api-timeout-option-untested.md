**Commit**: a6062d0868 · **Ticket**: KAFKA-17137
**Subsystem**: core/integration/kafka/api/PlaintextAdminIntegrationTest
**Category**: Missing null / bounds check

**Root cause**
Several Admin API integration test paths — `describeConfigs`, `alterConfigs`, ACL operations — lacked explicit tests confirming that a `timeoutMs(0)` option causes a `TimeoutException` instead of hanging. Without these timeout tests, a regression that ignores the timeout option would go undetected. The design gap was that only the happy-path and auth-failure paths were covered; the timeout-option contract was never verified.

**Before/After**
```java
// Before (no test for timeoutMs option on broken client)
// After
val exception = assertThrows(classOf[ExecutionException], () => {
    brokenClient.describeConfigs(configResources,
        new DescribeConfigsOptions().timeoutMs(0)).all().get()
})
assertInstanceOf(classOf[TimeoutException], exception.getCause)
```

**How to spot**
For every Admin API method that accepts an `Options` subclass, check whether the test suite includes a case where `timeoutMs(0)` is set and the broker is unreachable. Missing such a test means the option's contract is untested and a silent regression could go undetected.

**Test**
`testDescribeConfigWithOptionTimeoutMs`, `testAlterConfigsWithOptionTimeoutMs`, `testAclOperationsWithOptionTimeoutMs`, and `testDeleteAclsWithOptionTimeoutMs` added.
