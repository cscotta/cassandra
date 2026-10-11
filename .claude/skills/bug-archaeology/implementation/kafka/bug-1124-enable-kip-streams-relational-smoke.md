**Commit**: 53afb1e0c5 · **Ticket**: KAFKA-19202
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Enable next system test with KIP-1071. Also fixes the other KIP-1071 system tests, which now require enabling
the unstable `streams.version` feature. Reviewers: Bill Bejeck <bbejeck@apache.org>

**Before/After**
```java
// Before
                        mkEntry(StreamsConfig.STATE_DIR_CONFIG, stateDir)
                new KafkaStreams(getTopology(), getConfig(broker, application, id, processingGuarantee, stateDir));
                    final String stateDir = args[4];
                    App.startSync(kafka, UUID.randomUUID().toString(), nodeId, processingGuarantee, stateDir);
GROUP_COORDINATOR_REBALANCE_PROTOCOLS="group.coordinator.rebalance.protocols"

// After
                                           final String groupProtocol,
                        mkEntry(StreamsConfig.STATE_DIR_CONFIG, stateDir),
                        mkEntry(StreamsConfig.GROUP_PROTOCOL_CONFIG, groupProtocol)
                                             final String groupProtocol,
                new KafkaStreams(getTopology(), getConfig(broker, application, id, processingGuarantee, groupProtocol, stateDir));
                    final String groupProtocol = args[4];
                    final String stateDir = args[5];
                    App.startSync(kafka, UUID.randomUUID().toString(), nodeId, processingGuarantee, groupProtocol, stateDir);
                                            "classic",
UNSTABLE_FEATURE_VERSIONS_ENABLE = "unstable.feature.versions.enable"
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19202: enable kip-1071 in streams_relational_smoke_test (#19604).

**Test**
Test modified in RelationalSmokeTest.java covering the corrected behaviour.
