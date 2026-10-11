# Bug 0365: Race Condition in ReadCallback Signals DigestResolver Before Responses Are Visible

**Commit**: 93496e826e · **Ticket**: CASSANDRA-16807
**Subsystem**: src/java/org/apache/cassandra/service/reads/ReadCallback.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`ReadCallback.onResponse` maintained a separate `volatile int received` counter incremented with an `AtomicIntegerFieldUpdater`. The condition for signalling the `SimpleCondition` was `n >= blockFor && resolver.isDataPresent()`, where `n` was the locally-incremented counter value. Because the counter and the actual response storage in `DigestResolver.responses` were updated independently, the counter could reach `blockFor` and trigger `condition.signalAll()` *before* the responses that were just preprocessed by `DigestResolver` were published to other threads. The thread waiting in `awaitResults` would then observe `resolver.responses.size() < blockFor`, producing an incorrect result or an assertion failure.

### Before / After

```java
// BEFORE — uses a separate atomic counter independent of actual response storage
int n = waitingFor(message.from())
      ? recievedUpdater.incrementAndGet(this)
      : received;
if (n >= blockFor && resolver.isDataPresent())
    condition.signalAll();
```

```java
// AFTER — checks the resolver's own response collection size, guaranteeing
//          that all responses visible to the signal are also visible to waiters
resolver.preprocess(message);
if (resolver.isDataPresent() && resolver.responses.size() >= blockFor)
    condition.signalAll();
```

---

## How to Spot

Whenever a "count" variable is used to gate a signal/notify on a concurrent collection, check whether the count and the collection are kept in sync under the same memory visibility guarantee. If increments to the counter happen outside the collection's own internal publish barrier, a waiter can be woken up while the collection still appears smaller than the count, leading to stale reads.

---

## Test

`DigestResolverTest.multiThreadedNoRepairNeededReadCallback` — two threads race to call `onResponse` while a third waits in `awaitResults`, then asserts `resolver.isDataPresent()` and `resolver.responsesMatch()` are both true after the await returns.
