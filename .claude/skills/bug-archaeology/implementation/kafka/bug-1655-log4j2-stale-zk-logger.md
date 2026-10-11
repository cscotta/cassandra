## Commit

- Hash: f9b4cdbbc9
- Tickets: KAFKA-18485
- Subject: KAFKA-18485: Remove stale ZK-mode controller logger from log4j2.yaml

## Subsystem

config / logging

## Category

Wrong constant / default value

## Root cause

`config/log4j2.yaml` retained a `kafka.controller` logger entry that was only relevant to the ZooKeeper-based controller, which has been removed in the KRaft-only codebase. The dead entry caused confusion (it appeared to configure something that no longer exists) and was a maintenance hazard if future code reused the logger name.

## Before

```yaml
- name: kafka.controller
  level: TRACE
  additivity: false
  appenderRef:
    - ref: controllerAppender
```

## After

Entry removed entirely.

## How to spot

Logging configuration files that reference class/package paths no longer present in the codebase. Search `log4j2.yaml` logger `name` values against the current source tree; any that match no existing package are stale.

## Test

No automated test; verified by inspecting the active logger hierarchy after the removal.
