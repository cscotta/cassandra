# Bug 012: CMS Reconfiguration Fails When Paxos Repair is Disabled

**Commit**: 780274346018cf1c66dd1d1bdba0a227700d8cf4 · **Ticket**: CASSANDRA-20869
**Subsystem**: TCM (Topology Change Management) / Paxos Repair
**Category**: Logic error in condition

---

## Root Cause

`ActiveRepairService.repairPaxosForTopologyChangeAsync` returned a list containing a single no-op supplier (`() -> ImmediateFuture.success(null)`) when paxos repair was disabled or when there were no ranges to repair. The caller in `ReconfigureCMS` assigned this return value to a `List` variable and then entered a retry loop that attempted to call `remove(0)` on the list. Because `Arrays.asList(...)` returns a fixed-size list that does not support `remove`, this threw an `UnsupportedOperationException` at runtime, breaking the CMS reconfiguration entirely. The correct behavior for both early-exit paths is to signal "nothing to do" by returning an empty list.

### Before / After

```java
// BEFORE — returns fixed-size singleton list, removal causes UnsupportedOperationException
if (!paxosRepairEnabled())
{
    return Arrays.asList(() -> ImmediateFuture.success(null));
}
if (ranges.isEmpty())
{
    return Arrays.asList(() -> ImmediateFuture.success(null));
}

// ReconfigureCMS — assigned directly, retry loop calls remaining.remove(0)
List<Supplier<Future<?>>> remaining = ActiveRepairService.instance()
    .repairPaxosForTopologyChangeAsync(...);

// AFTER — returns mutable empty list; caller wraps result in Lists.newArrayList()
if (!paxosRepairEnabled())
    return Collections.emptyList();
if (ranges.isEmpty())
    return Collections.emptyList();

List<Supplier<Future<?>>> remaining = Lists.newArrayList(
    ActiveRepairService.instance().repairPaxosForTopologyChangeAsync(...));
```

---

## How to Spot

When a method returns a `List` that the caller is expected to mutate (e.g., by calling `remove`), every return site must return a mutable list. Returning `Arrays.asList(...)` or `Collections.singletonList(...)` for the "nothing to do" case is a common trap because those factory methods produce fixed-size or unmodifiable lists. During review, check that all code paths returning a list share the same mutability contract; if the caller performs structural mutations, every early-exit path must also return a mutable list (or an empty `Collections.emptyList()` if no entries are needed).

---

## Test

`testReconfigurePaxosRepairDisabled` spins up a 3-node cluster with `paxos_repair_enabled=false`, then calls `nodetool cms reconfigure 3` and asserts the command succeeds without error.
