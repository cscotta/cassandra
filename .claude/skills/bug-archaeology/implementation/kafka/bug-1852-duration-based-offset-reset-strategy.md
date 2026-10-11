**Commit**: d83f09d014 · **Ticket**: KAFKA-18015
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Part of KIP-1106. Adds the public APIs to Kafka Streams, to support the the newly added "by_duration" reset policy,
plus adds the missing "none" reset policy. Deprecates the enum `Topology.AutoOffsetReset` and
all related methods, and replaced them with new overload using the new `AutoOffsetReset` class. Co-authored-by: Matthias J.

**Before/After**
```java
// Before
        return stream(topics, Consumed.with(null, null, null, null));

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
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18015: add support for duration based offset reset strategy to kafka streams (#17973).

**Test**
Test modified in AutoOffsetResetTest.java covering the corrected behaviour.
