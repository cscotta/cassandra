## Commit

- Hash: 624dd45809
- Tickets: KAFKA-18321
- Subject: KAFKA-18321: Add StreamsGroupMember, MemberState, and Assignment classes for Streams group coordinator

## Subsystem

group-coordinator / streams / member management

## Category

Logic error in condition

## Root cause

The Streams group coordinator was accumulating member state as raw fields in multiple places without a unified representation. Without an `Assignment` record class, task assignments (active, standby, warm-up) were passed as separate maps that could be updated independently, making it easy to leave the three maps in an inconsistent state (e.g. promoting a warm-up task to active without clearing it from warm-up). The new `Assignment`, `MemberState`, and `StreamsGroupMember` classes enforce co-update of related fields.

## Before

Task assignment state stored as independent mutable maps; no encapsulation; inconsistent partial updates possible.

## After

```java
public record Assignment(
    Map<String, Set<Integer>> activeTasks,
    Map<String, Set<Integer>> standbyTasks,
    Map<String, Set<Integer>> warmupTasks) { }
```

`Assignment.EMPTY` sentinel; `MemberState` enum; `StreamsGroupMember` encapsulates all per-member coordinator state with `fromRecord()` factory.

## How to spot

Coordinator state where logically atomic multi-field updates (task promotion from warm-up to active requires updating two maps simultaneously) are scattered across independent mutable fields — they should be co-located in an immutable record that is replaced atomically.

## Test

`StreamsGroupMemberTest`, `MemberStateTest`, and `AssignmentTest` added with construction, equality, and factory-method assertions.
