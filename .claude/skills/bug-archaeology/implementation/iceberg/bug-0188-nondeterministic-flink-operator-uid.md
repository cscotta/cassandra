# Bug 0188: Non-deterministic operator UIDs in DynamicIcebergSink

**Commit**: 0e5428cdc41d591f84164073a25f54c2dd5df9b6 · **Ticket**: #15687

## Detection Rule

When a Flink operator UID is constructed by concatenating a runtime-variable `sinkId` with a fixed suffix, verify that the same `sinkId` is not also used as part of a separate UID that is composed differently, producing inconsistent naming across the operator graph.

## Code Smell

A `.uid(prefixIfNotNull(uidPrefix, sinkId + "-pre-commit-topology"))` call where `sinkId` is a non-fixed string derived at runtime, while all sibling operators in the same sink pipeline use UIDs constructed only from `uidPrefix` plus a fixed constant.

## Tags

- scope: project-specific
- category: wrong-constant
- code-pattern: non-deterministic-id-construction
- severity: crash
- subsystem: flink/sink/dynamic

## Root Cause

The pre-commit topology operator's UID was built by prepending `sinkId` (a value that contains the uidPrefix already baked in) before the fixed suffix `"-pre-commit-topology"`, whereas every other operator in the pipeline built its UID from `uidPrefix` alone plus a constant. This caused the pre-commit topology to receive a different UID than expected when the sink was restored from a checkpoint, making the pipeline non-deterministic across restarts and breaking savepoint compatibility.

## Example

```java
// BEFORE — sinkId already embeds uidPrefix, causing double-prefixing
.uid(prefixIfNotNull(uidPrefix, sinkId + "-pre-commit-topology"));
```

```java
// AFTER — consistent with all other operators: prefix + fixed constant only
.uid(prefixIfNotNull(uidPrefix, "-pre-commit-topology"));
```
