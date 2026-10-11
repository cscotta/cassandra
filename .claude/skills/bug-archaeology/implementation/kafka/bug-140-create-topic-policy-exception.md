**Commit**: bf15205647 · **Ticket**: KAFKA-19350
**Subsystem**: metadata/src/main/java/org/apache/kafka/controller/ReplicationControlManager.java
**Category**: Logic error in condition

**Root cause**
`ReplicationControlManager.validateNewTopic()` caught only `PolicyViolationException` from `CreateTopicPolicy.validate()`. Any other exception (e.g., a `RuntimeException` from a buggy policy implementation) propagated up uncaught and was handled by the `FatalFaultHandler`, crashing the controller. The fix adds a `catch (Throwable e)` block that converts arbitrary exceptions to `ApiError` (logging stack traces for unknown server errors) and returns them as a normal API error code instead of escalating to fatal.

**Before/After**
```java
// BEFORE – only PolicyViolationException caught; others crash controller
try {
    createTopicPolicy.get().validate(supplier.get());
} catch (PolicyViolationException e) {
    return new ApiError(Errors.POLICY_VIOLATION, e.getMessage());
}

// AFTER – all throwables handled as API errors
} catch (Throwable e) {
    ApiError apiError = ApiError.fromThrowable(e);
    if (apiError.error() == Errors.UNKNOWN_SERVER_ERROR) {
        log.error("Unknown server error validating Create Topic", e);
    }
    return apiError;
}
```

**How to spot**
Any `try/catch` block that only catches a specific checked exception from a user-provided plugin or policy interface but lets `RuntimeException` propagate should be treated as a potential controller-crash vector. Search for calls to configurable policy interfaces inside controller event handlers.

**Test**
`testCreateTopicsWithPolicyUnexpectedException`: a policy that throws `InvalidTopicException` for one topic and `RuntimeException` for another; asserts that topic creation returns `INVALID_TOPIC_EXCEPTION` and `UNKNOWN_SERVER_ERROR` respectively, without crashing the controller.
