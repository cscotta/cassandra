**Commit**: 95650431df · **Ticket**: KAFKA-16339
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
     * @see #flatTransform(TransformerSupplier, String...)
     * @see #flatTransform(TransformerSupplier, String...)
     * @see #flatTransform(TransformerSupplier, String...)
     * @see #flatTransform(TransformerSupplier, String...)
     * @see #flatTransform(TransformerSupplier, String...)
     * @see #flatTransform(TransformerSupplier, String...)
    /**
     * Transform each record of the input stream into zero or more records in the output stream (both key and value type
     * can be altered arbitrarily).
     * A {@link Transformer} (provided by the given {@link TransformerSupplier}) is applied to each input record and
     * returns zero or more output records.
     * Thus, an input record {@code <K,V>} can be transformed into output records {@code <K':V'>, <K'':V''>, ...}.

// After
// (no additions)
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16339: [2/4 kstream#flattransform] remove deprecated "transformer" methods and classes (#17245).

**Test**
Test modified in KStreamImplTest.java covering the corrected behaviour.
