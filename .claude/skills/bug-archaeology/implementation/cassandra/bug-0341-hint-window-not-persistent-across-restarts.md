# Bug 0341: Hint Window Not Persistent Across Node Restarts

**Commit**: b2ccd0f3f5 · **Ticket**: CASSANDRA-14309
**Subsystem**: src/java/org/apache/cassandra/hints/HintsService.java, HintsStore.java, HintsBuffer.java, StorageProxy.java
**Category**: State not cleaned up

---

## Root Cause

The `shouldHint()` check in `StorageProxy` measured how long a node had been continuously down via `Gossiper.getEndpointDowntime()`. This value is reset to zero each time the node restarts. A node that keeps bouncing could therefore always appear "recently down" and accumulate hints indefinitely even when the oldest stored hint for that node already exceeded `max_hint_window_in_ms`. The fix introduces a new `hint_window_persistent_enabled` configuration property (default `true`). When enabled, `shouldHint()` additionally retrieves the timestamp of the oldest hint on disk for the target node via `HintsService.getEarliestHintForHost()` and rejects new hints if the oldest existing hint is older than `max_hint_window_in_ms`, regardless of current downtime.

### Before / After

```java
// BEFORE – only checks current downtime, reset on each restart
boolean hintWindowExpired = Gossiper.instance.getEndpointDowntime(replica.endpoint()) > DatabaseDescriptor.getMaxHintWindow();
```

```java
// AFTER – also checks oldest stored hint timestamp
if (tryEnablePersistentWindow && !hintWindowExpired && DatabaseDescriptor.hintWindowPersistentEnabled())
{
    long earliestHint = HintsService.instance.getEarliestHintForHost(hostIdForEndpoint);
    hintWindowExpired = Clock.Global.currentTimeMillis() - maxHintWindow > earliestHint;
}
```

---

## How to Spot

Any expiry or rate-limiting mechanism that relies solely on an in-memory, restart-reset counter (like gossip downtime) is vulnerable to indefinite accumulation if the underlying resource (hint files) persists across restarts. Whenever a "window" check is backed by volatile runtime state, ask whether the durable state (files on disk) should also be consulted.

---

## Test

`HintsBufferTest.testEarliestHintTime()` and `HintsServiceTest.testEarliestHint()` verify that the earliest hint timestamp is correctly tracked in both the in-memory buffer and across a flush/close cycle.
