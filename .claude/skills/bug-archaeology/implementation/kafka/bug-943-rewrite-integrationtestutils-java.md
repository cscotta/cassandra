**Commit**: 46b474a9de · **Ticket**: KAFKA-19239
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This PR rewrites the IntegrationTestUtils.java from Scala to Java. ## Changes:

- Converted all the existing Scala code in IntegrationTestUtils.scala
into Java in IntegrationTestUtils.java. - Preserved the original logic and functionality to ensure backward
compatibility. - Updated relevant imports and dependencies accordingly.

**Before/After**
```java
// Before
import kafka.server.IntegrationTestUtils

      response = IntegrationTestUtils.connectAndReceive[InitProducerIdResponse](request,
        destination = broker,
        listenerName = listener)

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with

// After
import org.apache.kafka.server.IntegrationTestUtils
      val port = broker.boundPort(listener)
      response = IntegrationTestUtils.connectAndReceive[InitProducerIdResponse](request, port)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19239 rewrite integrationtestutils by java (#19776).

**Test**
Test modified in ProducerIntegrationTest.scala covering the corrected behaviour.
