**Commit**: 1c82b89b4c · **Ticket**: KAFKA-18712
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Ismael Juma <ismael@juma.me.uk>, Mickael Maison <mickael.maison@gmail.com>, Christo Lolov <lolovc@amazon.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    this(id, Seq(EndPoint(host, port, listenerName, protocol)), None, emptySupportedFeatures)
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software

// After
import org.apache.kafka.network.EndPoint
    this(id, Seq(new EndPoint(host, port, listenerName, protocol)), None, emptySupportedFeatures)
import org.apache.kafka.network.EndPoint
          config.effectiveAdvertisedBrokerListeners.map(_.toPublic()).asJava).
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18712 move endpoint to server module (#18803).

**Test**
Test modified in SocketServerTest.scala covering the corrected behaviour.
