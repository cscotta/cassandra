**Commit**: 482694c9d3 · **Ticket**: KAFKA-20173
**Subsystem**: streams/src
**Category**: Wrong serialization / deserialization

**Root cause**
Ensures that all Metered Window-stores (plain, ts, headers) pass headers
into de/serializers. Reviewers: Alieh Saeedi <asaeedi@confluent.io>, TengYao Chi
 <frankvicky@apache.org>, Bill Bejeck <bill@confluent.io>

**Before/After**
```java
// Before
                sendOldValues);
    protected byte[] serializeValue(final V value) {
        return value != null ? serdes.rawValue(value, internalContext.headers()) : null;
    }

    protected V deserializeValue(final byte[] rawValue) {
        return rawValue != null ? serdes.valueFrom(rawValue, internalContext.headers()) : null;
    }

            streamsMetrics,
            streamsMetrics,
            streamsMetrics,

// After
                sendOldValues
            );
    protected byte[] serializeValue(final V value) {
        return value != null ? serdes.rawValue(value, internalContext.headers()) : null;
    }

    protected V deserializeValue(final byte[] rawValue) {
        return rawValue != null ? serdes.valueFrom(rawValue, internalContext.headers()) : null;
    }

```

**How to spot**
When reviewing streams/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20173: ensure metered window-stores pass headers correctly (#21936).

**Test**
Test modified in KTableAggregateTest.java covering the corrected behaviour.
