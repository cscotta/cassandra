**Commit**: 2489cf586f · **Ticket**: KAFKA-16331
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Bill Bejeck <bill@confluent.io>

**Before/After**
```java
// Before
 * {@link #EXACTLY_ONCE "exactly_once"} (deprecated), or {@link #EXACTLY_ONCE_BETA "exactly_once_beta"} (deprecated), Kafka Streams does not
 * allow users to overwrite the following properties (Streams setting shown in parentheses):
     * <p>
     * Enabling exactly-once processing semantics requires broker version 0.11.0 or higher.
     * If you enable this feature Kafka Streams will use more resources (like broker connections)
     * compared to {@link #AT_LEAST_ONCE "at_least_once"} and {@link #EXACTLY_ONCE_V2 "exactly_once_v2"}.
     * @deprecated since 3.0.0, will be removed in 4.0. Use {@link #EXACTLY_ONCE_V2 "exactly_once_v2"} instead.
     */
    @SuppressWarnings("WeakerAccess")
    @Deprecated
    public static final String EXACTLY_ONCE = "exactly_once";


// After
 * Kafka Streams does not allow users to overwrite the following properties (Streams setting shown in parentheses):
     * <p> Enabling exactly-once-v2 requires broker version 2.5 or higher.

        " (Note, if <code>processing.guarantee</code> is set to <code>" + EXACTLY_ONCE_V2 + "</code>, the default value is <code>" + EOS_DEFAULT_COMMIT_INTERVAL_MS + "</code>," +
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16331: remove eosv1 config from streamsconfig (#17170).

**Test**
Test modified in StreamsConfigTest.java covering the corrected behaviour.
