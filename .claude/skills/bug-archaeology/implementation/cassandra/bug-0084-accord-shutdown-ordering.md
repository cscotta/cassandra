# Bug 0084: Accord Shut Down Before Messaging Service, Causing Message Drops During Drain

**Commit**: cc321720e3 · **Ticket**: n/a
**Subsystem**: service/StorageService, service/accord/AccordService, journal/Journal
**Category**: Logic error in condition

---

## Root Cause

`StorageService.drain()` previously shut down Accord (including its journal and command stores) before shutting down `MessagingService` and `ScheduledExecutors.optionalTasks`. This meant in-flight Accord messages arriving after `AccordService.shutdown()` were handled by partially-torn-down state, and tasks still scheduled in `optionalTasks` could attempt to write to a terminated journal. The fix reorders shutdown: Accord is stopped after `MessagingService` completes, and `ScheduledExecutors.optionalTasks` shuts down after `MessagingService` (not before), so no new work can arrive once executors drain. Separately, `Journal.isTerminated()` always returned `false`, preventing proper shutdown-state detection.

### Before / After

```java
// BEFORE – Accord shut down before MessagingService; ScheduledExecutors before Gossip
if (DatabaseDescriptor.getAccordTransactionsEnabled())
    AccordService.instance().shutdownAndWait(1, MINUTES);
...
ScheduledExecutors.optionalTasks.shutdown();
Gossiper.instance.stop();
...
// MessagingService shuts down here
```

```java
// AFTER – Accord shuts down after MessagingService; ScheduledExecutors after MessagingService
Gossiper.instance.stop();
...
if (AccordService.isSetup())
    AccordService.instance().shutdownAndWait(1, MINUTES);
// MessagingService shuts down
...
ScheduledExecutors.optionalTasks.shutdown();
```

---

## How to Spot

Shutdown sequences must mirror startup sequences in reverse. When auditing `drain()` or `shutdown()` methods, trace every component that can receive external messages or schedule new work and confirm it is torn down after its message source, not before. A component that checks `isTerminated()` and always returns `false` is a red flag for missed lifecycle transitions.

---

## Test

No test added.
