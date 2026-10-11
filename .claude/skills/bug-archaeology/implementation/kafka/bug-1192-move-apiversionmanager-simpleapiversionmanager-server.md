**Commit**: 5f2a68b150 · **Ticket**: KAFKA-19119
**Subsystem**: checkstyle/import-control-server.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Ken Huang <s7133700@gmail.com>, PoAn Yang
 <payang@apache.org>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.server.ApiVersionManager;
import kafka.server.{ApiVersionManager, BrokerReconfigurable, KafkaConfig}
import org.apache.kafka.server.ServerSocketFactory
      version => apiVersionManager.apiVersionResponse(throttleTimeMs = 0, version < 4)
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *

// After
    <allow pkg="org.apache.kafka.network.metrics" />
import org.apache.kafka.server.ApiVersionManager;
import kafka.server.{BrokerReconfigurable, KafkaConfig}
import org.apache.kafka.server.{ApiVersionManager, ServerSocketFactory}
      version => apiVersionManager.apiVersionResponse(0, version < 4)
```

**How to spot**
When reviewing checkstyle/import-control-server.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19119 move apiversionmanager/simpleapiversionmanager to server (#19426).

**Test**
Test modified in TestRaftRequestHandler.scala covering the corrected behaviour.
