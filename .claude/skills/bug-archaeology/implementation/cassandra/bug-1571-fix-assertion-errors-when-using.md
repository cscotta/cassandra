# Bug 1571: Fix Assertion errors when using empty clustering with static columns

**Commit**: a9c71e4a20 · **Ticket**: CASSANDRA-7455
**Subsystem**: cql3/statements / ColumnGroupMap
**Category**: Logic error in condition

---

## Root Cause

`ColumnGroupMap.Builder` groups incoming cells by their composite clustering key prefix. When a row contained static columns followed by regular columns that shared the same composite key bytes (which happens with empty clustering columns), the group-boundary check `isSameGroup(current)` did not account for the transition from a static group to a non-static group with identical key bytes. As a result, static and regular cells were incorrectly merged into the same `ColumnGroupMap`, triggering an `AssertionError` downstream.

The fix adds an explicit check: if the current group is static and the incoming cell is not a static-name composite, the group boundary is forced regardless of whether `isSameGroup` would otherwise say they belong together.

### Before / After

```java
// BEFORE — only checked key equality, missed static→non-static boundary
if (!isSameGroup(current))
{
    groups.add(currentGroup);
    ...
}
```

```java
// AFTER — also forces a new group when transitioning from static to non-static
if ((currentGroup.isStatic && !CompositeType.isStaticName(c.name())) || !isSameGroup(current))
{
    groups.add(currentGroup);
    ...
}
```

---

## How to Spot

When grouping heterogeneous cell types (static vs. regular) by a shared key, ensure the group-boundary predicate checks type identity in addition to key equality. A key that is structurally identical for both static and regular cells can otherwise suppress a required group split.

---

## Test

No test added in this commit.
