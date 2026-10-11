**Commit**: 79f979cd52 · **Ticket**: KAFKA-17372
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.server.quota.{ClientQuotaCallback, ClientQuotaEntity, ClientQuotaType}
/**
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements.  See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
  * The ASF licenses this file to You under the Apache License, Version 2.0
  * (the "License"); you may not use this file except in compliance with
  * the License.  You may obtain a copy of the License at
  *
  *    http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software

// After
import org.apache.kafka.server.quota.{ClientQuotaCallback, ClientQuotaEntity, ClientQuotaType, ThrottleCallback, ThrottledChannel}
import org.apache.kafka.server.quota.ThrottleCallback
import org.apache.kafka.server.quota.{ThrottleCallback, ThrottledChannel}
import org.apache.kafka.server.quota.ThrottleCallback
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17372 move `throttledchannelexpirationtest#testthrottledchanneldelay` and `throttledchannel` to server module (#16.

**Test**
Test modified in SocketServerTest.scala covering the corrected behaviour.
