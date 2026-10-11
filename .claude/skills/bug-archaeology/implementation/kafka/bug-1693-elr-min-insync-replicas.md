## Commit

- Hash: ec49a60e4f
- Tickets: KAFKA-16540
- Subject: KAFKA-16540: Enforce min.insync.replicas constraints when ELR feature is enabled

## Subsystem

controller / ActivationRecordsGenerator / ConfigurationControlManager

## Category

Missing null / bounds check

## Root cause

When the Eligible Leader Replicas (ELR) feature is enabled, `min.insync.replicas` must be set at the cluster level (not overrideable per-broker) to ensure the ELR election invariant holds across all partitions. Without enforcement:
1. A cluster could activate ELR without a cluster-level `min.insync.replicas` record, silently using broker defaults that might be 1 (disabling the protection ELR depends on).
2. A broker-level override could undermine the cluster-level policy after activation.
3. An admin could delete the cluster-level setting while ELR was active.

## Before

No auto-generation of the cluster-level record on activation; no block on broker-level overrides; no block on deletion.

## After

```java
// ActivationRecordsGenerator: auto-emit cluster min.insync.replicas on ELR feature activation
if (elrEnabled && !clusterMinInsyncReplicasPresent) {
    records.add(ConfigurationControlManager.buildClusterRecord(MIN_INSYNC_REPLICAS, "1"));
}
// ConfigurationControlManager: reject broker-level min.insync.replicas when ELR active
// ConfigurationControlManager: reject deletion of cluster-level min.insync.replicas when ELR active
```

## How to spot

Feature flags that have invariants dependent on other configuration values — activation logic must validate or auto-initialize those values; configuration management must block subsequent overrides that would violate the invariant.

## Test

`ActivationRecordsGeneratorTest` and `ConfigurationControlManagerTest` extended with ELR + `min.insync.replicas` interaction scenarios.
