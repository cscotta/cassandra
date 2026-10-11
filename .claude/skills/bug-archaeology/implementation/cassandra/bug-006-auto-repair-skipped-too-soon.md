# Bug 006: Fix initial auto-repairs skipped by too soon check

**Commit**: afa55123c8 · **Ticket**: CASSANDRA-21115
**Subsystem**: Auto-repair / AutoRepair
**Category**: Logic error in condition

---

## Root Cause

The `tooSoonToRunRepair` check used a single timestamp (`lastRepairTime`) that was overloaded to represent both the repair start time and the repair finish time. On the very first scheduling cycle after a node boot the in-memory field was zero, so the code loaded the finish timestamp from the history table. If a previous repair had completed recently, that timestamp caused the scheduler to decide it was "too soon" and skip the repair — even if the repair had never actually progressed past its start on this boot (i.e., the node had crashed mid-repair). The fix splits the single field into `lastRepairStartTime` and `lastRepairFinishTime`, and adds an explicit guard: if `startTime >= finishTime` (repair incomplete or unstarted) the check always returns `false`, allowing the repair to proceed or resume.

### Before / After

```java
// BEFORE — single field; incomplete repair looks like a completed one
if (repairState.getLastRepairTime() == 0)
    repairState.setLastRepairTime(AutoRepairUtils.getLastRepairTimeForNode(...));

long elapsed = timeFunc.get() - repairState.getLastRepairTime();
if (elapsed < config.getRepairMinInterval(...).toSeconds())
    return true; // skipped even if repair never finished

// AFTER — separate start/finish; in-progress repair always allowed
repairState.setLastRepairFinishTime(AutoRepairUtils.getLastRepairFinishTimeForNode(...));
repairState.setLastRepairStartTime(AutoRepairUtils.getLastRepairStartTimeForNode(...));

if (repairState.getLastRepairStartTime() >= repairState.getLastRepairFinishTime())
    return false; // incomplete/unstarted → allow resume

long elapsed = timeFunc.get() - repairState.getLastRepairFinishTime();
if (elapsed < config.getRepairMinInterval(...).toSeconds())
    return true;
```

---

## How to Spot

When a single timestamp field is used to gate a cooldown check, ask whether it always represents "work fully completed" or whether it can also reflect a partial or aborted attempt. Systems that persist both a start and a finish timestamp should expose and compare both; collapsing them into one creates an ambiguity where "recent start of a crashed job" is indistinguishable from "recent completion of a successful job", causing the scheduler to skip necessary work.

---

## Test

`AutoRepairTest.testTooSoonToRunRepairAllowsResumeOfInProgressRepair` inserts a history row with `start_ts > finish_ts` to simulate a crashed in-progress repair and asserts that `tooSoonToRunRepair` returns `false`, allowing the repair to resume.
