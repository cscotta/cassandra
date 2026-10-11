**Commit**: a244565ed2 · **Ticket**: KAFKA-18708
**Subsystem**: checkstyle/import-control-metadata.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.metadata.publisher.AclPublisher
          config,
import kafka.server.metadata.{ClientQuotaMetadataManager, DelegationTokenPublisher, DynamicClientQuotaPublisher, DynamicConfigPublisher, DynamicTopicClusterQuotaPublisher, KRaftMetadataCache, KRaftMetadataCachePublisher, ScramPublisher}
import org.apache.kafka.metadata.publisher.{AclPublisher, FeaturesPublisher}
        config,
import org.apache.kafka.metadata.publisher.AclPublisher
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See

// After
        <allow pkg="org.apache.kafka.security" />
        <allow pkg="org.apache.kafka.clients.admin" />
import org.apache.kafka.metadata.publisher.{AclPublisher, ScramPublisher}
          config.nodeId,
import kafka.server.metadata.{ClientQuotaMetadataManager, DelegationTokenPublisher, DynamicClientQuotaPublisher, DynamicConfigPublisher, DynamicTopicClusterQuotaPublisher, KRaftMetadataCache, KRaftMetadataCachePublisher}
import org.apache.kafka.metadata.publisher.{AclPublisher, FeaturesPublisher, ScramPublisher}
        config.nodeId,
import org.apache.kafka.metadata.publisher.{AclPublisher, ScramPublisher}
```

**How to spot**
When reviewing checkstyle/import-control-metadata.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18708: move scrampublisher to metadata module (#20468).

**Test**
Test modified in BrokerMetadataPublisherTest.scala covering the corrected behaviour.
