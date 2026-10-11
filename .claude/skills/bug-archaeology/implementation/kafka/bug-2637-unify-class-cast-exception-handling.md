**Commit**: 0eb9ac2bd0 · **Ticket**: KAFKA-16448
**Subsystem**: streams/src
**Category**: State not cleaned up

**Root cause**
Part of KIP-1033. Minor code cleanup. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
            throw createStreamsExceptionForKeyClassCastException(
            throw createStreamsExceptionForValueClassCastException(
    private <K> StreamsException createStreamsExceptionForKeyClassCastException(final String topic,
                                                                                final K key,
                                                                                final Serializer<K> keySerializer,
                                                                                final ClassCastException exception) {
        final String keyClass = key == null ? "unknown because key is null" : key.getClass().getName();
        return new StreamsException(
                String.format(
                        "ClassCastException while producing data to topic %s. " +
                                "The key serializer %s is not compatible to the actual key type: %s. " +
                                "Change the default key serde in StreamConfig or provide the correct key serde via method parameters " +

// After
import java.text.MessageFormat;
import java.util.Locale;
            throw createStreamsExceptionForClassCastException(
                ProductionExceptionHandler.SerializationExceptionOrigin.KEY,
            throw createStreamsExceptionForClassCastException(
                ProductionExceptionHandler.SerializationExceptionOrigin.VALUE,
    private <KV> StreamsException createStreamsExceptionForClassCastException(final ProductionExceptionHandler.SerializationExceptionOrigin origin,
                                                                              final String topic,
                                                                              final KV keyOrValue,
                                                                              final Serializer<KV> keyOrValueSerializer,
                                                                              final ClassCastException exception) {
        final String keyOrValueClass = keyOrValue == null
```

**How to spot**
When reviewing streams/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16448: unify class cast exception handling for both key and value (#16736).

**Test**
No test added.
