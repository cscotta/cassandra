**Commit**: 96036aee85 · **Ticket**: KAFKA-16916
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Fix to complete Future which was stuck due the exception.getCause() throws an error. The fix in the #16217 unblocked blocking thread but exception in catch block from blocking get call was wrapped in ExecutionException which is not the case when moved to async workflow hence getCause is not needed. Reviewers: Luke Chen <showuon@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                    completeAllExceptionally(topicFutures.values(), exception.getCause());
import org.junit.jupiter.api.Disabled;
    // The test runs forever on trunk after c01279b9. (KAFKA-16916)
    // We disable the test temporarily until we fix it to unblock the CI.
    @Disabled

// After
                    completeAllExceptionally(topicFutures.values(), exception);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16916: fixing error in completing future (#16249).

**Test**
Test modified in ClientAuthenticationFailureTest.java covering the corrected behaviour.
