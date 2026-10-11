**Commit**: fa682623b9 · **Ticket**: KAFKA-16666
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    testImplementation project(':group-coordinator')

// After
    implementation project(':group-coordinator')
    <allow pkg="org.apache.kafka.coordinator.group" />
  @Deprecated
            case "kafka.coordinator.group.GroupMetadataManager$OffsetsMessageFormatter":
                System.err.println("WARNING: kafka.coordinator.group.GroupMetadataManager$OffsetsMessageFormatter is deprecated and will be removed in the next major release. " +
                        "Please use org.apache.kafka.tools.consumer.OffsetsMessageFormatter instead");
                return className;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16666 migrate offsetmessageformatter to tools module (#16689).

**Test**
Test modified in ConsoleConsumerOptionsTest.java covering the corrected behaviour.
