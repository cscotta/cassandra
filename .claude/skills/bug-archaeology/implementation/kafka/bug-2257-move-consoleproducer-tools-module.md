**Commit**: 672c617233 · **Ticket**: KAFKA-14577
**Subsystem**: bin/kafka-console-producer.sh
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>, Federico Valeri <fedevaleri@gmail.com>

**Before/After**
```java
// Before
exec $(dirname $0)/kafka-run-class.sh kafka.tools.ConsoleProducer "$@"
"%~dp0kafka-run-class.bat" kafka.tools.ConsoleProducer %*
              files="(DefaultMessageFormatter|ProducerPerformance|StreamsResetter|Agent|TransactionalMessageCopier|ReplicaVerificationTool).java"/>
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0

// After
exec $(dirname $0)/kafka-run-class.sh org.apache.kafka.tools.ConsoleProducer "$@"
"%~dp0kafka-run-class.bat" org.apache.kafka.tools.ConsoleProducer %*
    <allow pkg="org.apache.kafka.tools" />
    <allow pkg="org.apache.kafka.tools.api" />
              files="(DefaultMessageFormatter|ProducerPerformance|StreamsResetter|Agent|TransactionalMessageCopier|ReplicaVerificationTool|LineMessageReader).java"/>
```

**How to spot**
When reviewing bin/kafka-console-producer.sh code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14577: move consoleproducer to tools module (#17157).

**Test**
Test modified in ConsoleProducerTest.scala covering the corrected behaviour.
