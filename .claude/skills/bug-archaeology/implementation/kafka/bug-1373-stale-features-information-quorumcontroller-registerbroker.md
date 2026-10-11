**Commit**: 88a23dab3e · **Ticket**: KAFKA-18844
**Subsystem**: metadata/src
**Category**: Logic error in condition

**Root cause**
In https://github.com/apache/kafka/pull/16848, we added `kraft.version`
to finalized features and got finalized features outside controller
event handling thread. This may make finalized features stale when
processing `registerBroker` event. Also, some cases like
`QuorumControllerTest.testBalancePartitionLeaders` become flaky cause of
outdated MV. This PR moves finalized features back to controller event
handling thread to avoid the error.

**Before/After**
```java
// Before
        // populate finalized features map with latest known kraft version for validation
        Map<String, Short> controllerFeatures = new HashMap<>(featureControl.finalizedFeatures(Long.MAX_VALUE).featureMap());
        controllerFeatures.put(KRaftVersion.FEATURE_NAME, raftClient.kraftVersion().featureLevel());
            () -> clusterControl.
                registerBroker(request, offsetControl.nextWriteOffset(),
                    new FinalizedControllerFeatures(controllerFeatures, Long.MAX_VALUE),
                    context.requestHeader().requestApiVersion() >= 3),

// After
            () -> {
                // Read and write data in the controller event handling thread to avoid stale information.
                Map<String, Short> controllerFeatures = new HashMap<>(featureControl.finalizedFeatures(Long.MAX_VALUE).featureMap());
                // Populate finalized features map with latest known kraft version for validation.
                controllerFeatures.put(KRaftVersion.FEATURE_NAME, raftClient.kraftVersion().featureLevel());
                return clusterControl.
                    registerBroker(request, offsetControl.nextWriteOffset(),
                        new FinalizedControllerFeatures(controllerFeatures, Long.MAX_VALUE),
                        context.requestHeader().requestApiVersion() >= 3);
            },
```

**How to spot**
When reviewing metadata/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18844: stale features information in quorumcontroller#registerbroker (#18997).

**Test**
No test added.
