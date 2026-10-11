## Commit

- Hash: 0b60d08d23
- Tickets: KAFKA-18341
- Subject: KAFKA-18341: Remove processRoles.isEmpty guard from protocol warning in KafkaConfig

## Subsystem

core / KafkaConfig / KRaft migration

## Category

Logic error in condition

## Root cause

The warning about unsupported `group.protocol` / `share.coordinator` settings was gated on `processRoles.isEmpty` (i.e. only shown in ZooKeeper mode). After ZooKeeper removal, `processRoles` is never empty, so the condition was always `false` and the warning was never emitted for any configuration. Users silently set unsupported protocols without any feedback.

## Before

```scala
if (processRoles.isEmpty && isConsumerProtocol(...)) {
    warn("...")
}
```

## After

```scala
if (isConsumerProtocol(...)) {
    warn("...")
}
```

## How to spot

Guard conditions that reference `processRoles.isEmpty` or ZK-mode flags that can never be true in a KRaft-only build — the entire branch body becomes dead code or the condition inverts silently.

## Test

`KafkaConfigTest` updated to verify the warning is emitted regardless of `processRoles`.
