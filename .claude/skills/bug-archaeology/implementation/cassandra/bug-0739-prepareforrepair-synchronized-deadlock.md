# Bug 0739: Deadlock — synchronized prepareForRepair blocks gossip; hardcoded 1-hour timeouts

**Commit**: 25580e3a7f · **Ticket**: CASSANDRA-9292
**Subsystem**: Repair / ActiveRepairService
**Category**: Deadlock

---

## Root Cause
`ActiveRepairService.prepareForRepair()` was declared `synchronized`, which meant that the entire prepare phase — including sending `PrepareMessage` to all endpoints and waiting up to one hard-coded hour for responses — held the intrinsic lock on the `ActiveRepairService` instance. Any gossip activity that needed to call into `ActiveRepairService` during that window would deadlock. Additionally, the hardcoded `TimeUnit.HOURS.toMillis(1)` timeout values for both the per-message `sendRR` call and the `prepareLatch.await` were unrelated to the configured RPC timeout, making the service unresponsive under network partition for up to an hour.

Both problems are fixed: `synchronized` is removed from the method signature (the `parentRepairSessions` map is already a `ConcurrentHashMap`), and the two hardcoded 1-hour values are replaced with `DatabaseDescriptor.getRpcTimeout()`.

### Before / After
```java
// BEFORE
public synchronized UUID prepareForRepair(...) {
    ...
    MessagingService.instance().sendRR(msg, neighbour, callback,
        TimeUnit.HOURS.toMillis(1), true);
    ...
    prepareLatch.await(1, TimeUnit.HOURS);
}

// AFTER
public UUID prepareForRepair(...) {
    ...
    MessagingService.instance().sendRR(msg, neighbour, callback,
        DatabaseDescriptor.getRpcTimeout(), true);
    ...
    prepareLatch.await(DatabaseDescriptor.getRpcTimeout(), TimeUnit.MILLISECONDS);
}
```

---

## How to Spot
Any `synchronized` method that performs remote I/O or blocks on a latch is a deadlock risk — the lock is held for the entire duration of the blocking call. In review, look for `synchronized` on public service methods that send network messages and verify that the state they protect is not already thread-safe (e.g., `ConcurrentHashMap`).

---

## Test
No new test added. The fix is structural: removing the lock and replacing magic timeout constants.
