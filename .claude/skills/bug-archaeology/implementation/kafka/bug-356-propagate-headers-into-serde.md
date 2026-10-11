**Commit**: c0d143dd76 · **Ticket**: KAFKA-20173
**Subsystem**: streams/src
**Category**: Wrong serialization / deserialization

**Root cause**
This PR fixes headers propagation in SessionWindowedSerializer and
TimeWindowedSerializer so they works as expected (from headers
propagation perspective). Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
import static org.mockito.ArgumentMatchers.eq;
        final List<String> data = List.of("test-key");
        verify(mockSerializer).serialize(eq(topic), eq(headers), eq("test-key"));
        verify(mockSerializer, never()).serialize(anyString(), anyString());
        return inner.serialize(topic, data.key());
        return inner.serialize(topic, data.key());

// After
        final String key = "test-key";
        final List<String> data = List.of(key);
        verify(mockSerializer).serialize(topic, headers, key);
        verify(mockSerializer, never()).serialize(topic, key);
        return serializeBaseKey(topic, new RecordHeaders(), data);
    }

    @Override
    public byte[] serializeBaseKey(final String topic, final Headers headers, final Windowed<T> data) {
        return inner.serialize(topic, headers, data.key());
        return serializeBaseKey(topic, new RecordHeaders(), data);
    }
```

**How to spot**
When reviewing streams/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20173: propagate headers into serde 4/n (#21755).

**Test**
Test modified in ListSerializerTest.java covering the corrected behaviour.
