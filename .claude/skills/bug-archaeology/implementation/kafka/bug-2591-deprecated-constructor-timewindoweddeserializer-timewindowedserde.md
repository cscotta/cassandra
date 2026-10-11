**Commit**: 242114eab9 · **Ticket**: KAFKA-12830
**Subsystem**: streams/src
**Category**: Wrong serialization / deserialization

**Root cause**
Signed-off-by: PoAn Yang <payang@apache.org>

Reviewers: Josep Prat <josep.prat@aiven.io>

**Before/After**
```java
// Before
    @Deprecated
    public TimeWindowedDeserializer(final Deserializer<T> inner) {
        this(inner, Long.MAX_VALUE);
    }

        @Deprecated
        public TimeWindowedSerde(final Serde<T> inner) {
            super(new TimeWindowedSerializer<>(inner.serializer()), new TimeWindowedDeserializer<>(inner.deserializer()));
        }

    /**
     * Construct a {@code TimeWindowedSerde} object for the specified inner class type.

// After
// (no additions)
```

**How to spot**
When reviewing streams/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-12830: remove deprecated constructor in timewindoweddeserializer and timewindowedserde (#16809).

**Test**
No test added.
