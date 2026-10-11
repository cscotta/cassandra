**Commit**: a9a4a52c9d · **Ticket**: KAFKA-16963
**Subsystem**: raft/src
**Category**: State not cleaned up

**Root cause**
Add a ducktape system test for KIP-853 quorum reconfiguration, including adding and removing voters. Reviewers: Colin P. McCabe <cmccabe@apache.org>

**Before/After**
```java
// Before
        // In summary, let's consider a replica caughed up for add voter, if they
                 use_new_coordinator=None
        :param int controller_num_nodes_override: the number of nodes to use in the cluster, instead of 5, 3, or 1 based on num_nodes, if positive, not using ZooKeeper, and isolated_kafka is not None; ignored otherwise
                    server_prop_overrides=server_prop_overrides
            controller_only_configs = {
                config_property.NODE_ID: self.idx(node) + config_property.FIRST_CONTROLLER_ID - 1,
            }
            kraft_controller_plus_zk_configs = controller_only_configs.copy()
            kraft_controller_plus_zk_configs.update(zk_broker_configs)
            kraft_controller_plus_zk_configs.pop(config_property.BROKER_ID)

// After
        // In summary, let's consider a replica caught up for add voter, if they
PROCESS_ROLES = "process.roles"
ADVERTISED_LISTENERS = "advertised.listeners"
LISTENERS = "listeners"
CONTROLLER_LISTENER_NAMES = "controller.listener.names"
                 use_new_coordinator=None,
                 dynamicRaftQuorum=False
        :param int controller_num_nodes_override: the number of controller nodes to use in the cluster, instead of 5, 3, or 1 based on num_nodes, if positive, not using ZooKeeper, and isolated_kafka is not None; ignored otherwise
            self.dynamicRaftQuorum = dynamicRaftQuorum
            self.first_controller_started = False
                    server_prop_overrides=server_prop_overrides, dynamicRaftQuorum=self.dynamicRaftQuorum
                controller_only_configs = {
```

**How to spot**
When reviewing raft/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16963: ducktape test for kip-853 (#17081).

**Test**
Test modified in config_property.py covering the corrected behaviour.
