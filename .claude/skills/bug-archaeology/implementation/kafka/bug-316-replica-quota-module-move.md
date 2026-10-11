## Commit
994eae3992

## Ticket
KAFKA-17840

## Subsystem
Broker / Quota

## Category
Refactor / module restructuring

## Root cause
`ReplicaQuota`, `ReplicationQuotaManager`, and related classes were in the `core` module. They are broker-server concerns and belong in the `server` module. Keeping them in `core` created a circular dependency risk and polluted the public `core` API surface.

## Before
```
core/src/main/scala/kafka/server/ReplicaQuota.scala
core/src/main/scala/kafka/server/ReplicationQuotaManager.scala
```

## After
Moved to `server` module; all import sites in `core` updated to new package.

## How to spot
Classes whose names suggest server-internal concerns (`Replica*`, `Quota*`, `Coordinator*`) should live in `server` or `coordinator-*` modules, not in `core`. Check `module-info.java` boundaries if they exist.

## Test
Existing quota and throttle tests continue to pass after the move.
