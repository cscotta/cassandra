**Commit**: 1134520dec · **Ticket**: KAFKA-16573
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Matthias J. Sax <matthias@confluent.io>, Bruno Cadonna <bruno@confluent.io>, Anna Sophie Blee-Goldman <ableegoldman@apache.org>

**Before/After**
```java
// Before
        if (keySerdeConfigSetting ==  null) {
            final Deserializer<?> contextKeyDeserializer = context.keySerde().deserializer();
            final Deserializer<?> contextValueDeserializer = context.valueSerde().deserializer();
            deserializerToUse = (Deserializer<T>) (isKey ? contextKeyDeserializer : contextValueDeserializer);
            final Serializer<?> contextKeySerializer = context.keySerde().serializer();
            final Serializer<?> contextValueSerializer = context.valueSerde().serializer();
            serializerToUse = (Serializer<T>) (isKey ? contextKeySerializer : contextValueSerializer);
            serdeToUse = (Serde<T>) (isKey ?  getter.keySerde() : getter.valueSerde());
        keySerializer = prepareKeySerializer(keySerializer, context, this.name());
        valSerializer = prepareValueSerializer(valSerializer, context, this.name());

// After
        if (keySerdeConfigSetting == null) {
            deserializerToUse = (Deserializer<T>) (isKey ? context.keySerde().deserializer() : context.valueSerde().deserializer());

            serializerToUse = (Serializer<T>) (isKey ? context.keySerde().serializer() : context.valueSerde().serializer());
            serdeToUse = (Serde<T>) (isKey ? getter.keySerde() : getter.valueSerde());


import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.streams.errors.StreamsException;
        try {
            keySerializer = prepareKeySerializer(keySerializer, context, this.name());
        } catch (ConfigException | StreamsException e) {
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16573: specify node and store where serdes are needed (#15790).

**Test**
Test modified in ProcessorNodeTest.java covering the corrected behaviour.
