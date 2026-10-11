**Commit**: 45b5df2bc7 · **Ticket**: KAFKA-20121
**Subsystem**: streams/src
**Category**: Wrong serialization / deserialization

**Root cause**
This PR adds the `ValueTimestampHeaders` and its related
serializer/deserializer as infrastructure of KIP-1271. Reviewers: Alieh Saeedi <asaeedi@confluent.io>, Matthias J. Sax
 <matthias@confluent.io>

**Before/After**
```java
// Before
// (no removals)

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
When reviewing streams/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20121: create valuetimestampheaders and its serializer/deserializer (#21408).

**Test**
Test modified in ValueTimestampHeadersTest.java covering the corrected behaviour.
