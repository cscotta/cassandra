**Commit**: af8d058d8e · **Ticket**: KAFKA-17491
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
import kafka.server.metadata.BrokerServerMetrics
import org.apache.kafka.server.metrics.KafkaYammerMetrics
          brokerMetrics = BrokerServerMetrics(metrics)
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0

// After
import org.apache.kafka.server.metrics.{BrokerServerMetrics, KafkaYammerMetrics}
          brokerMetrics = new BrokerServerMetrics(metrics)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17491: move brokerservermetrics to server module (#17114).

**Test**
Test modified in BrokerServerMetricsTest.scala covering the corrected behaviour.
