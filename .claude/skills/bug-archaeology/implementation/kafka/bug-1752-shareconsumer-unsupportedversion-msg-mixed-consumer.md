**Commit**: be4d1a6277 · **Ticket**: KAFKA-18135
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Add specific error handling for unsupported version in share consumer and consumer

Reviewers: Lianet Magrans <lmagrans@confluent.io>, Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.common.errors.UnsupportedVersionException;
import org.apache.kafka.common.protocol.ApiKeys;
        } else {
            if (isHBApiUnsupportedErrorMsg(exception)) {
                // This is expected to be the case where building the request fails because the node does not support
                // the API. Propagate custom message.
                handleFatalFailure(new UnsupportedVersionException(CONSUMER_PROTOCOL_NOT_SUPPORTED_MSG, exception));
            } else {
                // This is the case where building the request fails even though the node supports the API (ex.
                // required version 1 not available when regex in use).
                handleFatalFailure(exception);
            }

// After
        } else if (!handleSpecificFailure(exception)) {
            handleFatalFailure(exception);
                if (!handleSpecificExceptionInResponse(response, currentTimeMs)) {
    /**
     * Error handling specific failure to a group type when sending the request
     * and no response has been received.
     *
     * @param exception The exception thrown building the request
     * @return true if the error was handled, else false
     */
    public boolean handleSpecificFailure(Throwable exception) {
        return false;
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18135: shareconsumer hb unsupportedversion msg mixed with consumer hb (#18101).

**Test**
Test modified in ConsumerHeartbeatRequestManagerTest.java covering the corrected behaviour.
