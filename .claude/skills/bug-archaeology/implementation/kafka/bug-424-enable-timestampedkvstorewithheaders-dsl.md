**Commit**: 9f22628b8f · **Ticket**: KAFKA-20220
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This PR introduces the DslStoreFormat enum and extends DslKeyValueParams
to enable headers-aware key-value stores in the Kafka Streams DSL,
implementing the foundational infrastructure for  [

KIP-1285](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1285%3A+DSL+Opt-in+Support+for+Headers-Aware+State+Stores). Reviewers: Matthias J. Sax <matthias@confluent.io>, TengYao Chi
 <frankvicky@apache.org>

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
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20220: enable timestampedkvstorewithheaders in dsl (1/n) (#21572).

**Test**
Test modified in StreamsConfigTest.java covering the corrected behaviour.
