# Bug 0172: Primodal Epoch Re-initialization Fails on CAS Contention

**Commit**: b10e269344 · **Ticket**: CASSANDRA-19208
**Subsystem**: schema/DistributedMetadataLogKeyspace
**Category**: Logic error in condition

---

## Root Cause

`DistributedMetadataLogKeyspace.initialize()` inserts the first-ever metadata log entry via a Paxos-style lightweight transaction (`IF NOT EXISTS` / compare-and-swap). When a second node attempts the same initialization concurrently, the CAS fails and the driver returns the existing row in the result set alongside `[applied] = false`. The original code treated any `false` result as an error and threw `IllegalStateException("Could not initialize log.")`, breaking upgrade scenarios where multiple nodes race to initialize the log at startup. The fix checks whether the existing row already contains the expected first-epoch values, and if so, returns `true` (treating idempotent initialization as success), only throwing if the existing state is genuinely incompatible.

### Before / After

```java
// BEFORE
return result.one().getBoolean("[applied]");
```

```java
// AFTER
UntypedResultSet.Row row = result.one();
if (row.getBoolean("[applied]"))
    return true;
if (row.getLong("epoch") == FIRST.getEpoch() && ...correct sentinel values...)
    return true;
throw new IllegalStateException("Could not initialize log.");
```

---

## How to Spot

CAS operations that return the existing row on failure must check whether the pre-existing row is already in the desired state before treating the failure as an error. Whenever `[applied] = false` from a LWT is immediately converted to an exception without inspecting the returned row, there is a risk of spurious failures under concurrent init. Review all callers of `result.one().getBoolean("[applied]")` to ensure they handle the idempotent-already-done case.

---

## Test

No dedicated unit test added; the fix was validated by the Harry Upgrade integration test that exercises concurrent node startup.
