## Commit

- Hash: dbe27c9eb2
- Tickets: KAFKA-18467
- Subject: KAFKA-18467: Document that first replica in NewTopic is preferred leader

## Subsystem

clients / admin / NewTopic API

## Category

Logic error in condition

## Root cause

`NewTopic.replicasAssignments()` accepted a map from partition index to an ordered list of replica IDs but nowhere documented that the first replica in each list is treated as the preferred leader. Users who supplied replicas in the wrong order inadvertently created topics with non-preferred leaders, causing unnecessary leadership elections or unbalanced load — with no indication from the API that ordering mattered.

## Before

```java
/**
 * @return replica assignments by partition, or empty if not specified
 */
public Map<Integer, List<Integer>> replicasAssignments() { ... }
```

## After

```java
/**
 * @return replica assignments by partition, or empty if not specified.
 * The first replica will be treated as the preferred leader.
 */
public Map<Integer, List<Integer>> replicasAssignments() { ... }
```

## How to spot

Public API methods that accept ordered collections where ordering has semantic effect but the Javadoc says nothing about it — callers are forced to read internal implementation to understand the contract.

## Test

Documentation change only; no new test.
