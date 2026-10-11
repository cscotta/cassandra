## Commit

- Hash: b9a952df6c
- Tickets: KAFKA-18311 (4/N)
- Subject: KAFKA-18311: Add CopartitionedTopicsEnforcer for Streams group coordinator

## Subsystem

group-coordinator / streams / topic management

## Category

Logic error in condition

## Root cause

The Streams group coordinator lacked enforcement of the copartitioning constraint: all input topics in a co-grouped topology must have the same number of partitions. Without the enforcer, a topology with mismatched partition counts would be accepted and assigned, leading to incorrect task distribution at runtime where tasks expected identical partition-count across co-grouped topics.

## Before

No copartition validation at coordinator assignment time; mismatches silently propagated to task assignment.

## After

`CopartitionedTopicsEnforcer` validates that all topics in a copartition group share the same partition count; throws `TopologyException` listing the conflicting topics when they diverge.

## How to spot

Topology validation logic that only checks structural constraints (topic existence) but not numeric constraints (partition count alignment for co-grouping) will silently accept illegal topologies.

## Test

`CopartitionedTopicsEnforcerTest` covers same-count, different-count, and missing-topic scenarios.
