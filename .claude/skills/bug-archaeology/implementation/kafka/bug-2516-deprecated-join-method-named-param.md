**Commit**: a2f89f5412 · **Ticket**: KAFKA-16333
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
    /**
     * Join records of this {@code KTable} with another {@code KTable} using non-windowed inner join.
     * <p>
     * This is a foreign key join, where the joining key is determined by the {@code foreignKeyExtractor}.
     *
     * @param other               the other {@code KTable} to be joined with this {@code KTable}. Keyed by KO.
     * @param foreignKeyExtractor a {@link Function} that extracts the key (KO) from this table's value (V). If the
     *                            result is null, the update is ignored as invalid.
     * @param joiner              a {@link ValueJoiner} that computes the join result for a pair of matching records
     * @param named               a {@link Named} config used to name the processor in the topology
     * @param <VR>                the value type of the result {@code KTable}
     * @param <KO>                the key type of the other {@code KTable}

// After
// (no additions)
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16333: remove deprecated join method with named param (#16764).

**Test**
No test added.
