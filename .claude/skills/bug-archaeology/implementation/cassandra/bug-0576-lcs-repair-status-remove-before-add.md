# Bug 0576: LCS SSTables Duplicated When Repair Status Changes

**Commit**: f100024eb3 · **Ticket**: CASSANDRA-14720
**Subsystem**: src/java/org/apache/cassandra/db/compaction/CompactionStrategyManager.java
**Category**: State not cleaned up

---

## Root Cause

In `handleRepairStatusChangedNotification`, when an SSTable's repair status changes and it must migrate from one strategy holder to another, the code added the SSTable to the destination holder *before* removing it from all other holders. Because `addSSTables` in a Leveled Compaction Strategy can change the SSTable's level metadata, the subsequent `removeSSTables` call would no longer find the SSTable at its original location, leaving it tracked in both the source and destination holders simultaneously.

### Before / After

```java
// BEFORE
AbstractStrategyHolder dstHolder = holders.get(i);
dstHolder.addSSTables(group);          // level metadata mutated here

for (AbstractStrategyHolder holder : holders)
{
    if (holder != dstHolder)
        holder.removeSSTables(group);  // removal fails because level changed
}
```

```java
// AFTER
AbstractStrategyHolder dstHolder = holders.get(i);

for (AbstractStrategyHolder holder : holders)
{
    if (holder != dstHolder)
        holder.removeSSTables(group);  // remove first, before level changes
}

// adding sstables into another strategy may change its level,
// thus it won't be removed from original LCS. We have to remove sstables first
dstHolder.addSSTables(group);
```

---

## How to Spot

Whenever a move between data structures requires both a remove from the old location and an insert into the new location, order matters if the insert mutates the key/identity used for the later remove. Review every place that performs add-then-remove across holders or collections where the item's identity (e.g. LCS level) can change as a side-effect of the add; remove first to capture the original identity.

---

## Test

`testRepairStatusChanges` populates leveled SSTables, marks them repaired, then unrepaired, and asserts at each step that only the expected strategy holder contains sstables and the other is empty.
