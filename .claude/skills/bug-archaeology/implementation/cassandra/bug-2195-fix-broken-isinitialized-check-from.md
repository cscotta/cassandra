# Bug 2195: Node Marks Itself Initialized Before Joining Ring, Breaking Re-entry Guard

**Commit**: a850abd16e · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/service/StorageService.java
**Category**: Logic error in condition

---

## Root Cause

`StorageService.initServer()` was refactored to split local initialization (`initServerLocally`) from ring-joining (`maybeJoinRing`). In the original code, `initialized = true` was set at the top of the server-initialization block, which ran before the ring-join logic. After the refactor the `maybeJoinRing` method was given its own early-return guard that checks `initialized` to prevent duplicate ring joins; however, the line `initialized = true` remained in `initServerLocally` instead of being moved into `maybeJoinRing`. This meant `initialized` was already `true` by the time `maybeJoinRing` ran, so the guard immediately returned without the node ever joining the ring.

### Before / After

```java
// BEFORE – initialized = true set too early, in initServerLocally
initialized = true;   // line inside initServerLocally()
isClientMode = false;
// ...
// maybeJoinRing() checks `if (initialized) return;` – always exits immediately
```

```java
// AFTER – initialized = true moved into maybeJoinRing(), after the guard check
// (initServerLocally no longer sets it)
// maybeJoinRing():
if (initialized) { ... return; }
initialized = true;   // set here, only after confirming we haven't joined yet
```

---

## How to Spot

When refactoring a multi-step initialization sequence into separate methods, verify that each method's guard variable is set at the point in the new call chain where that step is actually completed, not where it was in the original monolithic method. Moving a method boundary past a state-flag assignment without also moving the assignment is a classic introduction of a broken initialization check.

---

## Test

No test added.
