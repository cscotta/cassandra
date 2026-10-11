**Commit**: a5e5e2dcd5 · **Ticket**: KAFKA-18706
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Move AclPublisher to org.apache.kafka.metadata.publisher package. Reviewers: Christo Lolov <lolovc@amazon.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
          authorizer
import kafka.server.metadata.{AclPublisher, ClientQuotaMetadataManager, DelegationTokenPublisher, DynamicClientQuotaPublisher, DynamicConfigPublisher, DynamicTopicClusterQuotaPublisher, KRaftMetadataCache, KRaftMetadataCachePublisher, ScramPublisher}
import org.apache.kafka.metadata.publisher.FeaturesPublisher
        authorizer
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *

// After
import org.apache.kafka.metadata.publisher.AclPublisher
          authorizer.toJava
import kafka.server.metadata.{ClientQuotaMetadataManager, DelegationTokenPublisher, DynamicClientQuotaPublisher, DynamicConfigPublisher, DynamicTopicClusterQuotaPublisher, KRaftMetadataCache, KRaftMetadataCachePublisher, ScramPublisher}
import org.apache.kafka.metadata.publisher.{AclPublisher, FeaturesPublisher}
        authorizer.toJava
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18706 move aclpublisher to metadata module (#18802).

**Test**
Test modified in BrokerMetadataPublisherTest.scala covering the corrected behaviour.
