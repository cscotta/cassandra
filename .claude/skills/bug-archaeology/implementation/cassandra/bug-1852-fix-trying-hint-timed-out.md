# Bug 1852: Fix Attempting to Hint Timed-Out Counter Writes

**Commit**: 3af19c4dd1 · **Ticket**: CASSANDRA-6322
**Subsystem**: net/WriteCallbackInfo
**Category**: Logic error in condition

---

## Root Cause

`WriteCallbackInfo.shouldHint()` decided whether a failed write should be stored as a hint for later replay. It correctly excluded `ConsistencyLevel.ANY` but did not exclude counter mutations. Counter mutations must not be hinted because hints are applied idempotently (last-write-wins), whereas counter increments are not idempotent — replaying a hinted counter increment would double-count the operation. The missing guard meant that a timed-out counter write could be enqueued as a hint and later applied a second time.

### Before / After

```java
// BEFORE
return consistencyLevel != ConsistencyLevel.ANY
       && StorageProxy.shouldHint(target);
```

```java
// AFTER
return sentMessage.verb != MessagingService.Verb.COUNTER_MUTATION
       && consistencyLevel != ConsistencyLevel.ANY
       && StorageProxy.shouldHint(target);
```

---

## How to Spot

Every hinting code path should be audited against the set of non-idempotent mutation types. Adding a new verb type to the messaging layer should trigger a review of all callers of `shouldHint()` to confirm the new type is handled correctly.

---

## Test

No test added.
