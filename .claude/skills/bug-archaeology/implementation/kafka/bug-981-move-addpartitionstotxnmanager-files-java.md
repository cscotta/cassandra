**Commit**: 83fb40d743 · **Ticket**: KAFKA-14895
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Move AddPartitionsToTxnManager to server module and convert to Java. This patch moves AddPartitionsToTxnManager from the core module to the
server module, with its package updated from `kafka.server` to
`org.apache.kafka.server.transaction`. Additionally, several
configuration used by AddPartitionsToTxnManager are moved from
KafkaConfig.scala to AbstractKafkaConfig.java. - brokerId
- requestTimeoutMs
- controllerListenerNames
- interBrokerListenerName
- interBrokerSecurityProtocol
- effectiveListenerSecurityProtocolMap

The next PR will move AddPartitionsToTxnManagerTest.scala to java

Reviewers: Justine Olshan <jolshan@confluent.io>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.server.{AddPartitionsToTxnManager, ReplicaManager}
    val controllerListenerName = new ListenerName(config.controllerListenerNames.head)
    val controllerSecurityProtocol = config.effectiveListenerSecurityProtocolMap.getOrElse(
      controllerListenerName,
      SecurityProtocol.forName(controllerListenerName.value())
    )
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with

// After
import kafka.server.ReplicaManager
import org.apache.kafka.server.transaction.AddPartitionsToTxnManager
    val controllerListenerName = new ListenerName(config.controllerListenerNames.get(0))
    val controllerSecurityProtocol = Option(config.effectiveListenerSecurityProtocolMap.get(controllerListenerName))
      .getOrElse(SecurityProtocol.forName(controllerListenerName.value()))
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14895 [1/n] move addpartitionstotxnmanager files to java (#19879).

**Test**
Test modified in QuorumTestHarness.scala covering the corrected behaviour.
