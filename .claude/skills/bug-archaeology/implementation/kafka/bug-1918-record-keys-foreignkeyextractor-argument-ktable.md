**Commit**: c76fb5cb9b · **Ticket**: KAFKA-17893
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Currently, KTable foreign key joins only allow extracting the foreign key from the value of the source record. This forces users to duplicate data that might already exist in the key into the value when the foreign key needs to be derived from both the key and value. This leads to:

- Data duplication
- Additional storage overhead
- Potential data inconsistency if the duplicated data gets out of sync
- Less intuitive API when the foreign key is naturally derived from both key and value

This change allows user to extract the foreign key from the key and value of the source record. Reviewers: Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
import java.util.function.BiFunction;
    /**
     * Join records of this {@code KTable} with another {@code KTable} using non-windowed inner join.
     * <p>
     * This is a foreign key join, where the joining key is determined by the {@code foreignKeyExtractor}.
     *
     * @param other               the other {@code KTable} to be joined with this {@code KTable}. Keyed by KO.
     * @param foreignKeyExtractor a {@link BiFunction} that extracts the key (KO) from this table's key and value (K, V). If the
     *                            result is null, the update is ignored as invalid.
     * @param joiner              a {@link ValueJoiner} that computes the join result for a pair of matching records
     * @param <VR>                the value type of the result {@code KTable}
     * @param <KO>                the key type of the other {@code KTable}
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17893: support record keys in the foreignkeyextractor argument of ktable foreign join (#17756).

**Test**
Test modified in KTableKTableForeignKeyJoinScenarioTest.java covering the corrected behaviour.
