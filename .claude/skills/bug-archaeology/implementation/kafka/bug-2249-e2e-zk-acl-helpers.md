**Commit**: 2733268409 · **Ticket**: KAFKA-17624
**Subsystem**: tests/kafkatest (kafka_acls.py, zookeeper.py, security_rolling_upgrade_test.py)
**Category**: State not cleaned up

**Root cause**
E2E test helpers `set_acls`, `add_read_acl`, `add_produce_acl`, `add_consume_acl`, `_add_acl_on_topic`, and `ZookeeperService.list_acls` all operated by invoking `AclCommand` with `--authorizer-properties zookeeper.connect=...`, which is the ZooKeeper-direct ACL path removed in 4.0. Similarly, `security_rolling_upgrade_test.py` contained `test_rolling_upgrade_phase_two` and `test_rolling_upgrade_sasl_mechanism_phase_two` tests that bootstrapped ACLs via ZooKeeper before starting the broker. Retaining these helpers and tests created dead code that could mislead operators trying to replicate test scenarios.

**Before/After**
```python
# Before – ZK-direct ACL methods
def set_acls(self, protocol, kafka, topic, group,
             force_use_zk_connection=False, ...):
    kafka.run_cli_tool(node,
        "kafka-acls --authorizer-properties zookeeper.connect=...")

# After – methods removed entirely
# add_cluster_acl and add_consume_acl remain (use bootstrap path)
```

**How to spot**
After the `--authorizer-properties` option is removed from `AclCommand`, any Python helper that constructs an `AclCommand` invocation containing `zookeeper.connect=` is broken; search for that string in the test tree to find all remaining dead helpers.

**Test**
No test added.
