**Commit**: 6b9ea76456 · **Ticket**: KAFKA-17057
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
Implements KIP-1065

Reviewers: Alieh Saeedi <asaeedi@confluent.io>, Bill Bejeck <bill@confluent.io>

**Before/After**
```java
// Before
        return ProductionExceptionHandlerResponse.FAIL;
        return ProductionExceptionHandlerResponse.FAIL;
}
        /* continue processing */
        /* fail processing */
        FAIL(1, "FAIL");
         * an english description of the api--this is for debugging and can change
         * the permanent and immutable id of an API--this can't change ever
        /* serialization exception occurred during serialization of the key */
        /* serialization exception occurred during serialization of the value */
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;

// After
import org.apache.kafka.common.errors.RetriableException;
        return exception instanceof RetriableException ?
            ProductionExceptionHandlerResponse.RETRY :
            ProductionExceptionHandlerResponse.FAIL;
        return exception instanceof RetriableException ?
            ProductionExceptionHandlerResponse.RETRY :
            ProductionExceptionHandlerResponse.FAIL;
}
        /** Continue processing.
         *
         * <p> For this case, output records which could not be written successfully are lost.
         * Use this option only if you can tolerate data loss.
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17057: add retry option to productionexceptionhanlder (#17163).

**Test**
Test modified in SwallowUnknownTopicErrorIntegrationTest.java covering the corrected behaviour.
