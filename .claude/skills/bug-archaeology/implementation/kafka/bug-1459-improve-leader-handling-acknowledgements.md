**Commit**: aa8c57665f · **Ticket**: KAFKA-18618
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, ShivsundarR <shr@confluent.io>, Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
     * <li> {@link AuthorizationException} if not authorized to the topic of group
            Exception exception = null;
            if (acknowledgements.getAcknowledgeErrorCode() != null) {
                exception = acknowledgements.getAcknowledgeErrorCode().exception();
            }
    // When the broker responds to the acknowledgements, this is the error code returned.
    private Errors acknowledgeErrorCode;

// After
import org.apache.kafka.common.errors.DisconnectException;
import org.apache.kafka.common.errors.NotLeaderOrFollowerException;
     * <li> {@link AuthorizationException} if not authorized to the topic or group
     * <li> {@link NotLeaderOrFollowerException} if the leader had changed by the time the acknowledgements were sent
     * <li> {@link DisconnectException} if the broker disconnected before the request could be completed
     * <p>Note that even if the exception is a retriable exception, the acknowledgement could not be completed and the
     * records need to be fetched again. The callback is called after any retries have been performed.
import org.apache.kafka.common.KafkaException;
            KafkaException exception = acknowledgements.getAcknowledgeException();
import org.apache.kafka.common.KafkaException;
    // When the broker responds to the acknowledgements, this is the exception thrown.
    private KafkaException acknowledgeException;
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18618: improve leader change handling of acknowledgements [1/n] (#18672).

**Test**
Test modified in AcknowledgementCommitCallbackHandlerTest.java covering the corrected behaviour.
