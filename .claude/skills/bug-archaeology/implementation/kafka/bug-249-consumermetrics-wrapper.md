**Commit**: c39e2af92c · **Ticket**: KAFKA-20408
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
https://issues.apache.org/jira/browse/KAFKA-20408

ConsumerMetrics which is redundant was referenced in
ClassicKafkaConsumer and AsyncKafkaConsumer

Updated those referenced classes, tests and build.gradle. Tried running the script `./gradlew genConsumerMetricsDocs` and
generated consumer_metrics.html looks good. Reviewers: Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
    mainClass = 'org.apache.kafka.clients.consumer.internals.ConsumerMetrics'
        ConsumerMetrics metricsRegistry = new ConsumerMetrics();
        this.fetchMetricsManager = new FetchMetricsManager(metrics, metricsRegistry.fetcherMetrics);
        ConsumerMetrics metricsRegistry = new ConsumerMetrics();
        this.fetchMetricsManager = new FetchMetricsManager(metrics, metricsRegistry.fetcherMetrics);
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at

// After
    mainClass = 'org.apache.kafka.clients.consumer.internals.FetchMetricsRegistry'
import static org.apache.kafka.clients.consumer.internals.ConsumerUtils.CONSUMER_METRIC_GROUP_PREFIX;
        FetchMetricsRegistry fetchMetricsRegistry = new FetchMetricsRegistry(CONSUMER_METRIC_GROUP_PREFIX);
        this.fetchMetricsManager = new FetchMetricsManager(metrics, fetchMetricsRegistry);
        FetchMetricsRegistry fetchMetricsRegistry = new FetchMetricsRegistry(CONSUMER_METRIC_GROUP_PREFIX);
        this.fetchMetricsManager = new FetchMetricsManager(metrics, fetchMetricsRegistry);
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20408 remove consumermetrics wrapper (#21974).

**Test**
No test added.
