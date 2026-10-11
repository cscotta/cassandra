**Commit**: 866f0cc308 · **Ticket**: KAFKA-16339
**Subsystem**: streams/src
**Category**: Incorrect filtering / wrong query result

**Root cause**
Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
 * {@link #process(ProcessorSupplier, String...) process(...)} and {@link #transformValues(ValueTransformerSupplier,
 * String...) transformValues(...)}.
     * @see #transformValues(ValueTransformerSupplier, String...)
     * @see #transformValues(ValueTransformerWithKeySupplier, String...)
     * @see #transformValues(ValueTransformerSupplier, String...)
     * @see #transformValues(ValueTransformerWithKeySupplier, String...)
     * {@link #transformValues(ValueTransformerSupplier, String...)} for stateful value transformation).
     * @see #transformValues(ValueTransformerSupplier, String...)
     * @see #transformValues(ValueTransformerWithKeySupplier, String...)
     * {@link #transformValues(ValueTransformerSupplier, String...)} for stateful value transformation).
     * @see #transformValues(ValueTransformerSupplier, String...)
     * @see #transformValues(ValueTransformerWithKeySupplier, String...)

// After
 * {@link #process(ProcessorSupplier, String...) process(...)} and {@link #processValues(FixedKeyProcessorSupplier,
 * String...) processValues(...)}.
     * @see #processValues(FixedKeyProcessorSupplier, String...)
     * @see #processValues(FixedKeyProcessorSupplier, String...)
     * {@link #processValues(FixedKeyProcessorSupplier, String...)} for stateful value processing).
     * @see #processValues(FixedKeyProcessorSupplier, String...)
     * {@link #processValues(FixedKeyProcessorSupplier, String...)} for stateful value processing).
     * @see #processValues(FixedKeyProcessorSupplier, String...)
     * {@link #processValues(FixedKeyProcessorSupplier, String...)} for stateful value processing).
     * @see #processValues(FixedKeyProcessorSupplier, String...)
```

**How to spot**
When reviewing streams/src code, look for incorrect filtering / wrong query result patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16339: [3/4 kstream#transformvalues] remove deprecated "transformer" methods and classes (#17266).

**Test**
Test modified in StreamsBuilderTest.java covering the corrected behaviour.
