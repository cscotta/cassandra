**Commit**: 7395d4e88f · **Ticket**: KAFKA-20158
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This PR adds `SessionStoreWithHeaders` interface and
`MeteredSessionStoreWithHeaders` and a unit test for
supporting KIP-1271. This is a stacked PR only review
2395a20

Reviewers: Alieh Saeedi <asaeedi@confluent.io>, TengYao Chi
 <frankvicky@apache.org>

**Before/After**
```java
// Before
    private final String metricsScope;
    private final Serde<K> keySerde;
    private final Serde<V> valueSerde;
    private final Time time;
    private StateSerdes<K, V> serdes;
    private StreamsMetricsImpl streamsMetrics;
    private Sensor putSensor;
    private Sensor fetchSensor;
    private Sensor flushSensor;
    private Sensor removeSensor;
    private Sensor e2eLatencySensor;
    private Sensor iteratorDurationSensor;

// After
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20158: adding sessionstorewithheaders, meteredsessionstore and tests (2/n) (#21513).

**Test**
Test modified in MeteredSessionStoreWithHeadersTest.java covering the corrected behaviour.
