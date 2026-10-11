**Commit**: 0600abdde3 · **Ticket**: KAFKA-19300
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
I verified the behavior by rewriting the
`GroupAuthorizerIntegrationTest` in Java in this PR:
https://github.com/apache/kafka/pull/19685  The state is now correct. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        if (previousState == MemberState.UNSUBSCRIBED) {
import org.junit.jupiter.api.{BeforeEach, TestInfo}

// After
        if (previousState == MemberState.UNSUBSCRIBED && maybeCompleteLeaveInProgress()) {
import org.junit.jupiter.api.{BeforeEach, TestInfo, Timeout}
  @Timeout(60)
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19300 asyncconsumer#unsubscribe always timeout due to groupauthorizationexception (#19779).

**Test**
Test modified in GroupAuthorizerIntegrationTest.scala covering the corrected behaviour.
