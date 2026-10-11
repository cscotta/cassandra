## Commit

- Hash: 3bda9f817d
- Tickets: KAFKA-18311 (3/N)
- Subject: KAFKA-18311: Add RepartitionTopics fix-point computation for Streams group coordinator

## Subsystem

group-coordinator / streams / topic management

## Category

Logic error in condition

## Root cause

The Streams group coordinator needed to compute partition counts for repartition topics (internal topics whose partition count depends on the source topics they repartition). If a repartition topic's partition count was not known (because it depended on another repartition topic whose count was not yet resolved), a simple single-pass scan would leave counts as `UNKNOWN`. The fix implements a fix-point iteration: keep resolving counts until no more changes occur, detecting unresolvable cycles as an error.

## Before

No repartition-topic resolution logic in the coordinator; unresolved counts silently used as `UNKNOWN`.

## After

`RepartitionTopics.computePartitionCounts()` uses a fix-point loop: each iteration resolves topics whose dependencies are now known; iteration stops when stable or a cycle is detected.

## How to spot

Single-pass dependency resolution that can leave items in an `UNKNOWN`/`null` state when there are chains of dependencies — fix-point iteration is required when the dependency graph can have arbitrary depth.

## Test

`RepartitionTopicsTest` covers direct resolution, chained resolution, cycle detection, and error cases.
