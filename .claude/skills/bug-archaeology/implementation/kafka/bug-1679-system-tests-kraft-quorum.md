## Commit

- Hash: 25fdcd05fc
- Tickets: KAFKA-17915
- Subject: KAFKA-17915: Migrate client compatibility system tests to isolated_kraft quorum

## Subsystem

tests / system tests / client compatibility

## Category

State not cleaned up

## Root cause

Several system tests (`client_compatibility_features_test.py`, `client_compatibility_produce_consume_test.py`, `quota_test.py`) still configured a ZooKeeper quorum for Kafka versions ≥ 3.3 (the first KRaft GA release). Running ZooKeeper-backed clusters in these tests was unnecessary and would break when ZooKeeper support is fully removed from the build. The tests were updated to use `isolated_kraft` quorum for any cluster at version 3.3 or later.

## Before

`quorum_type = 'zk'` used unconditionally regardless of Kafka version under test.

## After

```python
quorum_type = 'isolated_kraft' if version >= KafkaVersion('3.3') else 'zk'
```

## How to spot

System tests that hardcode `quorum_type = 'zk'` without a version gate — on a KRaft-only build they will fail to start the cluster because ZooKeeper support is absent.

## Test

The migrated tests pass in CI against KRaft clusters; ZK variant retained for testing older versions.
