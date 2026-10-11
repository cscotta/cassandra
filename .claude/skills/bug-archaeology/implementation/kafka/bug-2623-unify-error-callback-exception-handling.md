**Commit**: 7c539f9029 · **Ticket**: KAFKA-16448
**Subsystem**: streams/src
**Category**: Wrong serialization / deserialization

**Root cause**
Follow up code cleanup for KIP-1033. This PR unifies the handling of both error cases for exception handlers:
 - handler throws an exception
 - handler returns null

The unification happens for all 5 handler cases:
 - deserialzation
 - production / serialization
 - production / send
 - processing
 - punctuation

Reviewers:  Sebastien Viale <sebastien.viale@michelin.com>, Loic Greffier <loic.greffier@michelin.com>, Bill Bejeck <bill@confluent.io>

**Before/After**
```java
// Before
     * @deprecated Since 3.9. Use Please {@link #handle(ErrorHandlerContext, ConsumerRecord, Exception)}
     * @deprecated Since 3.9. Use {@link #handle(ErrorHandlerContext, ProducerRecord, Exception)} instead.
        super(exception);
                    } catch (final Exception deserializationException) {

// After
     * @deprecated Since 3.9. Use {@link #handle(ErrorHandlerContext, ConsumerRecord, Exception)} instead.
     * @deprecated Since 3.9. Use {@link #handleSerializationException(ErrorHandlerContext, ProducerRecord, Exception, SerializationExceptionOrigin)} instead.
    @Override
    public String toString() {
        // we do exclude headers on purpose, to not accidentally log user data
        return "ErrorHandlerContext{" +
            "topic='" + topic + '\'' +
            ", partition=" + partition +
            ", offset=" + offset +
            ", processorNodeId='" + processorNodeId + '\'' +
            ", taskId=" + taskId +
            '}';
```

**How to spot**
When reviewing streams/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16448: unify error-callback exception handling (#16745).

**Test**
Test modified in EosIntegrationTest.java covering the corrected behaviour.
