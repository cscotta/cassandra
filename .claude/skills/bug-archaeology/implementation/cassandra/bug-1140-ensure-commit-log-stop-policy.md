# Bug 1140: Commit Log Stop Policy Not Enforced at Startup

**Commit**: 3b7934f1aa · **Ticket**: CASSANDRA-8515
**Subsystem**: src/java/org/apache/cassandra/utils/JVMStabilityInspector.java
**Category**: Wrong constant / default value

---

## Root Cause

`JVMStabilityInspector.inspectCommitLogThrowable()` dispatched commit log errors by consulting the configured `CommitFailurePolicy` — which could be `ignore`, `stop`, or `die` — without first checking whether the daemon had finished initializing. During startup replay, a commit log error would therefore be handled according to whatever policy the operator had chosen for normal operation. A policy of `ignore` would silently swallow a fatal startup error and allow the node to start with an incomplete or corrupted dataset instead of aborting with a clear error message.

### Before / After

```java
// BEFORE
public static void inspectCommitLogThrowable(Throwable t)
{
    if (DatabaseDescriptor.getCommitFailurePolicy() == Config.CommitFailurePolicy.die)
        killer.killCurrentJVM(t);
    else
        inspectThrowable(t);
}
```

```java
// AFTER
public static void inspectCommitLogThrowable(Throwable t)
{
    if (!StorageService.instance.isSetupCompleted())
    {
        logger.error("Exiting due to error while processing commit log during initialization.", t);
        killer.killCurrentJVM(t, true);
    } else if (DatabaseDescriptor.getCommitFailurePolicy() == Config.CommitFailurePolicy.die)
        killer.killCurrentJVM(t);
    else
        inspectThrowable(t);
}
```

---

## How to Spot

Error-handling code that branches on a runtime-configurable policy should be audited for whether that policy is appropriate in all lifecycle phases. Startup is a distinct phase where "safe" policies such as `ignore` are semantically dangerous; the invariant "if we haven't finished initializing, any fatal error must abort" needs to be checked before policy dispatch. Anytime a `CommitFailurePolicy` (or similar) is consulted, verify there is a pre-check guarding the pre-startup case.

---

## Test

Four new tests in `CommitLogTest` cover the startup gate: `testCommitFailurePolicy_mustDieIfNotStartedUp` (policy=ignore, startup incomplete → must kill), `testCommitLogFailureBeforeInitialization_mustKillJVM` (low-level segment manager failure before startup), `testCommitLogFailureAfterInitialization_mustRespectFailurePolicy` (policy=ignore, after startup → must not kill), and the existing `die`/`stop` tests were updated to register a completed daemon.
