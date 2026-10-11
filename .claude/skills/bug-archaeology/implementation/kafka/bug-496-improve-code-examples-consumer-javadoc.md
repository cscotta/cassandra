**Commit**: 7157c05cc9 · **Ticket**: KAFKA-20065
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
The code examples in the javadoc often include HTML escaping for double
quotes and angle brackets. This can be avoided by tagging the code
examples as code, as opposed to just preformatted. The benefit is that
the code examples are much more readable in the source code, which makes
them much easier for the code authors to read and get correct. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>

**Before/After**
```java
// Before
 *     <li>For synchronous behaviour use {@link KafkaFuture#get()}
 *     props.setProperty(&quot;bootstrap.servers&quot;, &quot;localhost:9092&quot;);
 *     props.setProperty(&quot;group.id&quot;, &quot;test&quot;);
 *     props.setProperty(&quot;enable.auto.commit&quot;, &quot;true&quot;);
 *     props.setProperty(&quot;auto.commit.interval.ms&quot;, &quot;1000&quot;);
 *     props.setProperty(&quot;key.deserializer&quot;, &quot;org.apache.kafka.common.serialization.StringDeserializer&quot;);
 *     props.setProperty(&quot;value.deserializer&quot;, &quot;org.apache.kafka.common.serialization.StringDeserializer&quot;);
 *     KafkaConsumer&lt;String, String&gt; consumer = new KafkaConsumer&lt;&gt;(props);
 *     consumer.subscribe(Arrays.asList(&quot;foo&quot;, &quot;bar&quot;));
 *         ConsumerRecords&lt;String, String&gt; records = consumer.poll(Duration.ofMillis(100));
 *         for (ConsumerRecord&lt;String, String&gt; record : records)
 *             System.out.printf(&quot;offset = %d, key = %s, value = %s%n&quot;, record.offset(), record.key(), record.value());

// After
 *     <li>For synchronous behavior, use {@link KafkaFuture#get()}.
 * {@code
 *     props.setProperty("bootstrap.servers", "localhost:9092");
 *     props.setProperty("group.id", "test");
 *     props.setProperty("enable.auto.commit", "true");
 *     props.setProperty("auto.commit.interval.ms", "1000");
 *     props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
 *     props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
 *     KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
 *     consumer.subscribe(Arrays.asList("foo", "bar"));
 *         ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
 *         for (ConsumerRecord<String, String> record : records)
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20065: improve code examples in consumer javadoc (#21299).

**Test**
No test added.
