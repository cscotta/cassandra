**Commit**: 30a988a768 · **Ticket**: KAFKA-8666
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
     * An internal name will be used for the store.

// After
 * <p>
 * <b>Correct Usage When Providing Serde:</b>
 * <p>
 * To configure both the name of the store and the {@link Serde} for the key and value,
 * you should use the following pattern:
 * <pre>{@code
 * Materialized.<KeyType, ValueType, StateStore>as("MyStoreName")
 *     .withKeySerde(keySerde)
 *     .withValueSerde(valueSerde);
 * }</pre>
 * This ensures that the store name is retained while configuring the key and value serde.
 * <p>
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-8666: improve documentation on usage of materialized config object (#17145).

**Test**
No test added.
