**Commit**: b6d5f0556c · **Ticket**: KAFKA-16448
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
This PR is part of KIP-1033 which aims to bring a ProcessingExceptionHandler to Kafka Streams in order to deal with exceptions that occur during processing. This PR expose the new ErrorHandlerContext as a parameter to the Production exception handler and deprecate the previous handle signature. Co-authored-by: Dabz <d.gasparina@gmail.com>
Co-authored-by: loicgreffier <loic.greffier@michelin.com>

Reviewers: Bruno Cadonna <bruno@confluent.io>, Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
    /**
     * Return the non-deserialized byte[] of the input message key if the context has been triggered by a message.
     *
     * <p> If this method is invoked within a {@link Punctuator#punctuate(long)
     * punctuation callback}, or while processing a record that was forwarded by a punctuation
     * callback, it will return {@code null}.
     *
     * <p> If this method is invoked in a sub-topology due to a repartition, the returned key would be one sent
     * to the repartition topic.
     *
     * @return the raw byte of the key of the source message
     */

// After
    @Deprecated
    @Override
    public ProductionExceptionHandlerResponse handle(final ErrorHandlerContext context,
                                                     final ProducerRecord<byte[], byte[]> record,
                                                     final Exception exception) {
        return ProductionExceptionHandlerResponse.FAIL;
    }

     * @deprecated Since 3.9. Use {@link #handle(ErrorHandlerContext, ProducerRecord, Exception)} instead.
    @Deprecated
    default ProductionExceptionHandlerResponse handle(final ProducerRecord<byte[], byte[]> record,
                                                      final Exception exception) {
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16448: add errorhandlercontext in production exception handler (#16433).

**Test**
Test modified in AlwaysContinueProductionExceptionHandler.java covering the corrected behaviour.
