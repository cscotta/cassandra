**Commit**: 9a8fd60fa2 · **Ticket**: KAFKA-19932
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
adding handling of OOM and avoiding wrapped as timeout

**Before/After**
```java
// Before
// (no removals)

// After
        @SuppressWarnings("NPathComplexity")
            // Don't mask VirtualMachineError as TimeoutException - propagate it directly
            if (throwable instanceof VirtualMachineError) {
                handleFailure(throwable);
                return;
            }

    /**
     * Test that OutOfMemoryError is properly propagated and not masked as TimeoutException.
     * This test simulates an OOM error during response processing and verifies it propagates
     * without being wrapped. This is a regression test for KAFKA-19932.
     */
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19932: adding handling of oom and avoiding wrapped as timeout (#21117).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
