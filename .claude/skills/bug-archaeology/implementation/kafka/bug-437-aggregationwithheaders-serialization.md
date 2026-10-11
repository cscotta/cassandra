**Commit**: 7b46af69aa · **Ticket**: KAFKA-20158
**Subsystem**: streams/src
**Category**: Wrong serialization / deserialization

**Root cause**
This PR introduces `AggregationWithHeaders`  and serialization  support
introduced in KIP-1271 for storing session aggregations with  headers.

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
When reviewing streams/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20158: add aggregationwithheaders, serialization support and tests (1/n) (#21511).

**Test**
Test modified in AggregationWithHeadersTest.java covering the corrected behaviour.
