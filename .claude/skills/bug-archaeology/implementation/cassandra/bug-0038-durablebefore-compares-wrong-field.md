# Bug 0038: DurableBefore.fullyContainedIn Compares Wrong Field

**Commit**: 49b99c9319 · **Ticket**: CASSANDRA-20786
**Subsystem**: service/accord
**Category**: Logic error in condition

---

## Root Cause

`DurableBefore.fullyContainedIn` was supposed to determine whether a range of transaction IDs was fully covered by the durable-before watermark, but it was comparing the wrong field — using a timestamp or range boundary instead of the `txnId` itself. This caused the GC logic to incorrectly believe certain transactions were covered by the durability watermark when they were not (or vice versa), leading to premature or missed garbage-collection of journal entries. A related sub-bug: messages with nullable state were reading the state and then using it without re-checking for cancellation in between, creating a TOCTOU window.

### Before / After

The primary fix is in the `accord` submodule (not directly visible in the Cassandra diff), but the Cassandra-side change guards `ensureDurable` with a feature flag to avoid triggering the broken path:

```java
// BEFORE: AccordCommandStore.ensureDurable — unconditionally runs
protected void ensureDurable(Ranges ranges, RedundantBefore onCommandStoreDurable) {
    long reportId = nextDurabilityLoggingId.incrementAndGet();
    ...
}
```

```java
// AFTER: guarded to prevent invocation when linearizability violation reporting is disabled
protected void ensureDurable(Ranges ranges, RedundantBefore onCommandStoreDurable) {
    if (!CommandsForKey.reportLinearizabilityViolations())
        return;
    ...
}
```

---

## How to Spot

In range/interval containment checks, verify that the field being compared is the one that actually defines the relevant ordering for the check. Using a related but different field (e.g., a timestamp vs. a transaction ID that carries epoch+flags+timestamp) silently passes the wrong boundary to the comparison, producing subtly incorrect GC eligibility decisions without any obvious error at runtime.

---

## Test

`JournalGCTest` was fixed and tightened: it now snapshots the maximum `TxnId` before compaction and asserts all journal entries up to that ID are GC'd within a small number of cycles.
