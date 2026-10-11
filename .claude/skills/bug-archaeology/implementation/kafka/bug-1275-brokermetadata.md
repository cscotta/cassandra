**Commit**: d497250c22 · **Ticket**: KAFKA-18999
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
* Replace `BrokerMetadata` with `UsableBroker` in KRaftMetadataCache and
ReassignPartitionsCommand. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.admin.BrokerMetadata
  private def getAliveBrokers(image: MetadataImage): util.List[BrokerMetadata] = {
    image.cluster().brokers().values().stream()
      .filter(Predicate.not(_.fenced))
      .map(broker => new BrokerMetadata(broker.id, broker.rack))
      .collect(Collectors.toList())
  }

    val aliveBrokers = getAliveBrokers(image)
      util.Optional.of(aliveBrokers.get(ThreadLocalRandom.current().nextInt(aliveBrokers.size)).id)
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more

// After
    val aliveBrokers = image.cluster().brokers().values().stream()
      .filter(Predicate.not(_.fenced))
      .map(_.id()).toList
      util.Optional.of(aliveBrokers.get(ThreadLocalRandom.current().nextInt(aliveBrokers.size)))
        List<UsableBroker> usableBrokers = getBrokerMetadata(adminClient, brokersToReassign, enableRackAwareness);
        Map<TopicPartition, List<Integer>> proposedAssignments = calculateAssignment(currentAssignments, usableBrokers);
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18999 remove brokermetadata (#19227).

**Test**
Test modified in ReassignPartitionsUnitTest.java covering the corrected behaviour.
