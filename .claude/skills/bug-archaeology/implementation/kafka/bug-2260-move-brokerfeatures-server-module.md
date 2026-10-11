**Commit**: 10a0905628 · **Ticket**: KAFKA-17564
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
          brokerFeatures.defaultFinalizedFeatures
                map(kv => (kv._1, kv._2.toShort)).toMap))
        val newFinalizedFeature = Utils.mkMap(Utils.mkEntry(update.feature, newVersion)).asScala.toMap
import org.apache.kafka.server.ClientMetricsManager
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *

// After
import org.apache.kafka.server.BrokerFeatures
          brokerFeatures.defaultFinalizedFeatures.asScala.map { case (k, v) => (k, v.shortValue()) }
                map(kv => (kv._1, kv._2.toShort: java.lang.Short)).toMap.asJava))
        val newFinalizedFeature = Utils.mkMap(Utils.mkEntry(update.feature, newVersion: java.lang.Short))
import org.apache.kafka.server.{BrokerFeatures, ClientMetricsManager}
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17564 move brokerfeatures to server module (#17228).

**Test**
Test modified in ApiVersionManagerTest.scala covering the corrected behaviour.
