**Commit**: 8059503601 · **Ticket**: KAFKA-20132
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This PR implements the metered layer of the
TimestampedKeyValueStoreWithHeaders introduced in KIP-1271. Reviewers: TengYao Chi <frankvicky@apache.org>, Matthias J. Sax
 <matthias@confluent.io>

**Before/After**
```java
// Before
    private Sensor putIfAbsentSensor;
            maybeMeasureLatency(() -> wrapped().put(keyBytes(key), serdes.rawValue(value)), time, putSensor);
        return value != null ? serdes.valueFrom(value) : null;
        return Bytes.wrap(serdes.rawKey(key));

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
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20132: implement timestampedkeyvaluestorewithheaders (3/n) (#21451).

**Test**
Test modified in MeteredTimestampedKeyValueStoreWithHeadersTest.java covering the corrected behaviour.
