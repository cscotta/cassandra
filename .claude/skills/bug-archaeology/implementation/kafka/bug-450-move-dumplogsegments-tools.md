**Commit**: 434ae67d47 · **Ticket**: KAFKA-14579
**Subsystem**: bin/kafka-dump-log.sh
**Category**: Logic error in condition

**Root cause**
Move DumpLogSegments from core to tools and rewrite by java

Reviewers: Mickael Maison <mickael.maison@gmail.com>, Chih-Yuan Chien
<joshua2519@gmail.com>

**Before/After**
```java
// Before
exec $(dirname $0)/kafka-run-class.sh kafka.tools.DumpLogSegments "$@"
              files="VerifiableConsumer.java"/>
              files="(AclCommand|DefaultMessageFormatter|ProducerPerformance|StreamsResetter|Agent|TransactionalMessageCopier|ReplicaVerificationTool|LineMessageReader|ConsoleProducer).java"/>
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
exec $(dirname $0)/kafka-run-class.sh org.apache.kafka.tools.DumpLogSegments "$@"
    implementation project(':storage:storage-api')
    <allow pkg="org.apache.kafka.coordinator.common.runtime" />
    <allow pkg="org.apache.kafka.coordinator.share" />
    <allow pkg="org.apache.kafka.snapshot" />
    <allow pkg="org.apache.kafka.server.storage.log" />
    <allow pkg="org.apache.kafka.storage.log.metrics" />
    <suppress checks="ClassFanOutComplexity" files="DumpLogSegmentsTest"/>
              files="(VerifiableConsumer|DumpLogSegmentsTest).java"/>
              files="(AclCommand|DefaultMessageFormatter|ProducerPerformance|StreamsResetter|Agent|TransactionalMessageCopier|ReplicaVerificationTool|LineMessageReader|ConsoleProducer|DumpLogSegments).java"/>
```

**How to spot**
When reviewing bin/kafka-dump-log.sh code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14579: move dumplogsegments to tools (#21223).

**Test**
Test modified in DumpLogSegmentsTest.scala covering the corrected behaviour.
