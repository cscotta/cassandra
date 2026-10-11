**Commit**: 57eb5fd7dc · **Ticket**: KAFKA-14587
**Subsystem**: bin/kafka-acls.sh
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
exec $(dirname $0)/kafka-run-class.sh kafka.admin.AclCommand "$@"
"%~dp0kafka-run-class.bat" kafka.admin.AclCommand %*
              files="(ConsoleConsumer|DefaultMessageFormatter|StreamsResetter|ProducerPerformance|Agent).java"/>
              files="(DefaultMessageFormatter|ProducerPerformance|StreamsResetter|Agent|TransactionalMessageCopier|ReplicaVerificationTool|LineMessageReader).java"/>
     *        a client request to create ACLs. This may be null if ACLs are created directly in ZooKeeper
     *        using AclCommand.
     *        a client request to delete ACLs. This may be null if ACLs are deleted directly in ZooKeeper
     *        using AclCommand.
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.

// After
exec $(dirname $0)/kafka-run-class.sh org.apache.kafka.tools.AclCommand "$@"
"%~dp0kafka-run-class.bat" org.apache.kafka.tools.AclCommand %*
    <allow pkg="org.apache.kafka.metadata.authorizer" />
    <allow pkg="org.apache.kafka.security.authorizer" />
              files="(AclCommand|ConsoleConsumer|DefaultMessageFormatter|StreamsResetter|ProducerPerformance|Agent).java"/>
              files="(AclCommand|DefaultMessageFormatter|ProducerPerformance|StreamsResetter|Agent|TransactionalMessageCopier|ReplicaVerificationTool|LineMessageReader).java"/>
     *        a client request to create ACLs.
     *        a client request to delete ACLs.
```

**How to spot**
When reviewing bin/kafka-acls.sh code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14587: move aclcommand to tools (#17880).

**Test**
Test modified in EndToEndAuthorizationTest.scala covering the corrected behaviour.
