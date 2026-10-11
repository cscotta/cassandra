**Commit**: 8c71ab03b5 · **Ticket**: KAFKA-19176
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
### Problem
- Currently, when a transactional producer encounters retriable errors
(like `COORDINATOR_LOAD_IN_PROGRESS`) and exhausts all retries, finally
returns retriable error to Application Layer. - Application reties can cause duplicate records. As a fix we are
transitioning all retriable errors  as Abortable Error in transaction
producer path. - Additionally added InvalidTxnStateException as part of
https://issues.apache.org/jira/browse/KAFKA-19177

### Solution
- Modified the TransactionManager to automatically transition retriable
errors to abortable errors after all retries are exhausted.

**Before/After**
```java
// Before


// After
import org.apache.kafka.common.errors.InvalidTxnStateException;
import org.apache.kafka.common.errors.TransactionAbortableException;
            // RetriableExceptions from the Sender thread are converted to Abortable errors
            // because they indicate that the transaction cannot be completed after all retry attempts.
            // This conversion ensures the application layer treats these errors as abortable,
            // preventing duplicate message delivery.
            if (exception instanceof RetriableException ||
                    exception instanceof InvalidTxnStateException) {
                exception = new TransactionAbortableException("Transaction Request was aborted after exhausting retries.", exception);
            }


```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19176: update transactional producer to translate retriable into abortable exceptions (#19522).

**Test**
Test modified in SenderTest.java covering the corrected behaviour.
