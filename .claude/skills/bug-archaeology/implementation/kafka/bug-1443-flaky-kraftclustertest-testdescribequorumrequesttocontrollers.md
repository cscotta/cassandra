**Commit**: 63fc9b3cb8 · **Ticket**: KAFKA-18771
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
The case testDescribeQuorumRequestToControllers shutdowns raft client but not the controller. This makes client has chance to send a request to the controller and get NOT_LEADER_OR_FOLLOWER error. However, if the raft client finishes shutdown before handling the request, the request will not be handled. Shutdown the controller before doing KafkaFuture#get for the client request, so we can make sure the request is handled by another controller eventually.

**Before/After**
```java
// Before
        val quorumInfo2 = admin.describeMetadataQuorum(new DescribeMetadataQuorumOptions)
          .quorumInfo().get()

// After
        // Stop raft client but not the controller, because we would like to get NOT_LEADER_OR_FOLLOWER error first.
        // If the controller is shutdown, the client can't send request to the original leader.
        val quorumInfo2Future = admin.describeMetadataQuorum(new DescribeMetadataQuorumOptions).quorumInfo
        // If raft client finishes shutdown before returning NOT_LEADER_OR_FOLLOWER error, the request will not be handled.
        // This makes test fail. Shutdown the controller to make sure the request is handled by another controller.
        cluster.controllers.get(quorumInfo.leaderId).shutdown()
        val quorumInfo2 = quorumInfo2Future.get
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18771: fix flaky test kraftclustertest .testdescribequorumrequesttocontrollers (#18859).

**Test**
Test modified in KRaftClusterTest.scala covering the corrected behaviour.
