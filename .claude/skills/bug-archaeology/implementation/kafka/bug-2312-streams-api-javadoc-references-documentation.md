**Commit**: 235cafa805 · **Ticket**: KAFKA-6197
**Subsystem**: docs/api.html
**Category**: Logic error in condition

**Root cause**
Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
	The <a href="#streamsapi">Streams</a> API allows transforming streams of data from input topics to output topics.
	<a href="/{{version}}/javadoc/index.html?org/apache/kafka/streams/KafkaStreams.html" title="Kafka {{dotVersion}} Javadoc">javadocs</a>
	To use Kafka Streams DSL for Scala for Scala {{scalaVersion}} you can use the following maven dependency:
    <h2><a id="streams" href="/documentation/streams">9. Kafka Streams</a></h2>
    <p>Learn More about Kafka Streams read <a href="/documentation/streams">this</a> Section.</p>
                <li><a href="/{{version}}/documentation/streams">2.3 Streams API</a>
        <li><a href="/{{version}}/documentation/streams">9. Kafka Streams</a>

// After
	The <a href="/{{version}}/documentation/streams">Streams</a> API allows transforming streams of data from input topics to output topics.
	<a href="/{{version}}/javadoc/index.html?org/apache/kafka/streams/KafkaStreams.html" title="Kafka {{dotVersion}} Javadoc">javadocs</a>.
	To use Kafka Streams DSL for Scala {{scalaVersion}} you can use the following maven dependency:
    <h2><a id="streams" href="#streams">9. Kafka Streams</a></h2>
    <p>To learn more about Kafka Streams, visit the <a href="/documentation/streams">Kafka Streams page</a>.</p>
                <a href="/{{version}}/javadoc/index.html?org/apache/kafka/streams/KafkaStreams.html">Javadoc</a>
                <li><a href="#streamsapi">2.3 Streams API</a>
        <li><a href="#streams">9. Kafka Streams</a>
```

**How to spot**
When reviewing docs/api.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-6197: update streams api and javadoc references in documentation (#17215).

**Test**
No test added.
