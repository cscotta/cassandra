**Commit**: 3f465fc1b6 · **Ticket**: KAFKA-19202
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
New system test for KIP-1071. Standby replicas need to be enabled via `kafka-configs.sh`. Reviewers: Bill Bejeck <bill@confluent.io>, Matthias J. Sax
 <matthias@confluent.io>

**Before/After**
```java
// Before
        streamsProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-streams-standby-tasks");
        self.kafka.run_features_command("upgrade", "streams.version", 1)
    @matrix(metadata_quorum=[quorum.isolated_kraft])
    def test_standby_tasks_rebalance(self, metadata_quorum):
            group_protocol="classic",
            ",sourceTopic=%s,sinkTopic1=%s,sinkTopic2=%s,internal.task.assignor.class=org.apache.kafka.streams.processor.internals.assignment.LegacyStickyTaskAssignor" % (

// After
        self.use_streams_groups = True
        if self.use_streams_groups:
            self.kafka.run_features_command("upgrade", "streams.version", 1)
    def configure_standby_replicas(self, group_id, num_standby_replicas):
        force_use_zk_connection = not self.kafka.all_nodes_configs_command_uses_bootstrap_server()
        node = self.kafka.nodes[0]
        cmd = "%s --alter --add-config streams.num.standby.replicas=%d --entity-type groups --entity-name %s" % \
              (
                    self.kafka.kafka_configs_cmd_with_optional_security_settings(node, force_use_zk_connection),
                    num_standby_replicas,
                    group_id
              )
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19202: enable kip-1071 in streams_standby_replica_test.py (#19625).

**Test**
Test modified in StreamsStandByReplicaTest.java covering the corrected behaviour.
