# Bug 0840: nodetool repair exits with 0 for some errors

**Commit**: f092f7be2f · **Ticket**: CASSANDRA-12508
**Subsystem**: Tools / nodetool
**Category**: Logic error in condition

---

## Root Cause
`RepairRunner` listens for JMX progress events and signals completion when it sees a `COMPLETE` event. However, when an `ERROR` event arrived it was printed to the output stream but the `error` field was never set. Because only `COMPLETE` triggers the condition signal, the runner eventually returned normally with exit code 0 even though repair had failed, making the error invisible to scripts and CI systems.

### Before / After
```java
// BEFORE
out.println(message);
if (type == ProgressEventType.COMPLETE)
{
    condition.signalAll();

// AFTER
out.println(message);
if (type == ProgressEventType.ERROR)
{
    error = new RuntimeException("Repair job has failed with the error message: " + message);
}
if (type == ProgressEventType.COMPLETE)
{
    condition.signalAll();
```

---

## How to Spot
Whenever a tool wraps JMX/notification listeners and exits via a completion signal, check that error-event branches set the appropriate exit-state field before the completion path runs. A pattern like `if (type == COMPLETE) { signal }` with no corresponding `if (type == ERROR)` branch is a strong indicator that error events are silently discarded.

---

## Test
No test added (JMX notification handling is difficult to unit-test without a live server).
