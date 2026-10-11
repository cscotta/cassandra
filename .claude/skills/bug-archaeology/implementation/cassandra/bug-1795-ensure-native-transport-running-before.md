# Bug 1795: Ensure Native Transport Is Running Before Setting isRunning

**Commit**: 1b858be482 · **Ticket**: CASSANDRA-6618
**Subsystem**: src/java/org/apache/cassandra/transport/Server.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`Server.start()` called `run()` to bind the CQL port and then immediately set `isRunning` to `true` in the calling thread. However, `run()` bootstraps a Netty pipeline asynchronously; if the bind failed or had not yet completed, `isRunning` was already `true`, causing subsequent health checks or callers of `isRunning()` to believe the server was ready when it was not. The fix moves `isRunning.set(true)` to the end of the `run()` method itself, after `bootstrap.bind()` has returned a bound `Channel`, so the flag is only set once the server is genuinely listening.

### Before / After

```java
// BEFORE – flag set in start(), before run() fully completes
run();
isRunning.set(true);
```

```java
// AFTER – flag set inside run(), after the channel is actually bound
Channel channel = bootstrap.bind(socket);
connectionTracker.allChannels.add(channel);
isRunning.set(true);
```

---

## How to Spot

State flags that reflect whether an asynchronous or multi-step initialisation has completed must be set at the end of that initialisation, not by the caller. Review every `isRunning`/`isReady`/`isStarted` flag to confirm it is written only after the last observable side-effect of startup has succeeded.

---

## Test

No test added.
