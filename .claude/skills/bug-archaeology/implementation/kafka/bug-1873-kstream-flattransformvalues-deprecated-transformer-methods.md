**Commit**: d5c2029434 · **Ticket**: KAFKA-16339
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reviewer: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before

    /**
     * @throws IllegalArgumentException if the same instance is obtained each time
     */
    @SuppressWarnings("deprecation")
    public static <VR, V> void checkSupplier(final org.apache.kafka.streams.kstream.ValueTransformerSupplier<V, VR> supplier) {
        if (supplier.get() == supplier.get()) {
            final String supplierClass = supplier.getClass().getName();
            throw new IllegalArgumentException(String.format("%s generates single reference." +
                    " %s#get() must return a new object each time it is called.", supplierClass, supplierClass));
        }
    }

// After
     * @see #processValues(FixedKeyProcessorSupplier, Named, String...)
     * @see #processValues(FixedKeyProcessorSupplier, Named, String...)
     * @see #processValues(FixedKeyProcessorSupplier, Named, String...)
     * @see #processValues(FixedKeyProcessorSupplier, Named, String...)
     * @see #processValues(FixedKeyProcessorSupplier, Named, String...)
     * @see #processValues(FixedKeyProcessorSupplier, Named, String...)
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16339: [4/4 kstream#flattransformvalues] remove deprecated "transformer" methods and classes (#17882).

**Test**
Test modified in StreamsBuilderTest.java covering the corrected behaviour.
