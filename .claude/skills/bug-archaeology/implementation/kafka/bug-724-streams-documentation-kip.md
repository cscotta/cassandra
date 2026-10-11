**Commit**: 0ddc69da70 · **Ticket**: KAFKA-19721
**Subsystem**: docs/ops.html
**Category**: Logic error in condition

**Root cause**
Update KIP-1147 changes (renaming --property to --formatter-property) in
the ops and streams documentation. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
<pre><code class="language-bash">$ bin/kafka-console-consumer.sh --topic tieredTopic --from-beginning --max-messages 1 --bootstrap-server localhost:9092 --property print.offset=true</code></pre>
    --property print.key=true \
    --property print.value=true \
    --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
    --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer</code></pre>
    --property print.key=true \
    --property print.value=true \
    --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
    --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
    --property print.key=true \
    --property print.value=true \
    --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \

// After
<pre><code class="language-bash">$ bin/kafka-console-consumer.sh --topic tieredTopic --from-beginning --max-messages 1 --bootstrap-server localhost:9092 --formatter-property print.offset=true</code></pre>
--formatter-property print.key=true \
--formatter-property key.deserializer=org.apache.kafka.streams.kstream.TimeWindowedDeserializer \
--formatter-property key.deserializer.windowed.inner.deserializer.class=org.apache.kafka.common.serialization.StringDeserializer \
--formatter-property key.deserializer.window.size.ms=500
--formatter-property print.key=true \
--formatter-property key.deserializer=org.apache.kafka.streams.kstream.SessionWindowedDeserializer \
--formatter-property key.deserializer.windowed.inner.deserializer.class=org.apache.kafka.common.serialization.StringDeserializer</code></pre>
    --formatter-property print.key=true \
    --formatter-property print.value=true \
    --formatter-property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
    --formatter-property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer</code></pre>
```

**How to spot**
When reviewing docs/ops.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19721: update streams documentation with kip-1147 changes (#20606).

**Test**
No test added.
