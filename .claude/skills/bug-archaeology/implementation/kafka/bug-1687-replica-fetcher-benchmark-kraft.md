## Commit

- Hash: 7275dc129e
- Tickets: KAFKA-17730
- Subject: KAFKA-17730: Migrate ReplicaFetcherThreadBenchmark to KRaft metadata cache

## Subsystem

jmh-benchmarks / ReplicaFetcherThreadBenchmark

## Category

State not cleaned up

## Root cause

`ReplicaFetcherThreadBenchmark` set up its simulated broker environment using `ZkMetadataCache`, `UpdateMetadataRequest` (the ZK inter-broker control-plane request), and `KafkaZkClient`. With ZooKeeper support removed, the benchmark could not compile or run. The fix replaces the ZK setup with a KRaft-based `KRaftMetadataCache` populated via the `createPartition` helper, keeping the benchmark functional.

## Before

```java
ZkMetadataCache metadataCache = new ZkMetadataCache(...);
KafkaZkClient zkClient = ...;
UpdateMetadataRequest umr = ...;
metadataCache.updateMetadata(LeaderEpoch, umr);
```

## After

```java
KRaftMetadataCache metadataCache = new KRaftMetadataCache(...);
metadataCache.createPartition(partitionRecord);
```

## How to spot

JMH benchmarks or tests that instantiate `ZkMetadataCache`, `KafkaZkClient`, or construct `UpdateMetadataRequest` for setup purposes — these are ZK-only APIs that must be replaced with KRaft equivalents.

## Test

Benchmark compiles and runs successfully; throughput baseline unchanged.
