## Commit

- Hash: 5acbd42dd7
- Tickets: KAFKA-18277
- Subject: KAFKA-18277: Replace ZookeeperService with KRaft controllers in network degradation test

## Subsystem

tests / system tests / network degradation

## Category

State not cleaned up

## Root cause

`network_degrade_test.py` used `ZookeeperService` nodes as the target for network partition injection, testing broker behaviour when ZooKeeper became unreachable. With ZooKeeper removed from Kafka, the test was broken: no `ZookeeperService` exists in a KRaft cluster, so the network degradation scenario was never exercised. The fix replaces ZooKeeper nodes with KRaft controller nodes as the degradation target.

## Before

```python
self.zk = ZookeeperService(...)
self.kafka = KafkaService(..., zk=self.zk)
# degrade self.zk network
```

## After

```python
self.kafka = KafkaService(..., quorum_type='isolated_kraft')
# degrade controller node network
```

## How to spot

System tests that instantiate `ZookeeperService` for the purpose of fault injection rather than for cluster operation — these tests are broken in a KRaft-only build and need to be retargeted at controller nodes.

## Test

Network degradation test now runs against KRaft controller nodes in CI.
