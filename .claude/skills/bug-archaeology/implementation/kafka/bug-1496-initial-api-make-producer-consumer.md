**Commit**: 71314739f9 · **Ticket**: KAFKA-15995
**Subsystem**: checkstyle/import-control.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Greg Harris <gharris1727@gmail.com>, Luke Chen <showuon@gmail.com>

**Before/After**
```java
// Before
            this.interceptors = new ConsumerInterceptors<>(interceptorList);
            this.deserializers = new Deserializers<>(config, keyDeserializer, valueDeserializer);
                    Arrays.asList(deserializers.keyDeserializer, deserializers.valueDeserializer));
        this.interceptors = new ConsumerInterceptors<>(Collections.emptyList());
        this.deserializers = new Deserializers<>(keyDeserializer, valueDeserializer);
            this.interceptors = new ConsumerInterceptors<>(interceptorList);
            this.deserializers = new Deserializers<>(config, keyDeserializer, valueDeserializer);

// After
    <subpackage name="internals">
      <allow pkg="org.apache.kafka.common.metrics" />
      <allow pkg="org.apache.kafka.common.metrics.internals" />
    </subpackage>

 * Implement {@link org.apache.kafka.common.metrics.Monitorable} to enable the interceptor to register metrics. The following tags are automatically added to
 * all metrics registered: <code>config</code> set to <code>interceptor.classes</code>, and <code>class</code> set to the ConsumerInterceptor class name.
            this.interceptors = new ConsumerInterceptors<>(interceptorList, metrics);
            this.deserializers = new Deserializers<>(config, keyDeserializer, valueDeserializer, metrics);
                    Arrays.asList(deserializers.keyDeserializer(), deserializers.valueDeserializer()));
        this.interceptors = new ConsumerInterceptors<>(Collections.emptyList(), metrics);
        this.deserializers = new Deserializers<>(keyDeserializer, valueDeserializer, metrics);
```

**How to spot**
When reviewing checkstyle/import-control.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15995: initial api + make producer/consumer plugins monitorable (#17511).

**Test**
Test modified in KafkaConsumerTest.java covering the corrected behaviour.
