# Bug 0764: Thrift server stop before listen causes infinite loop

**Commit**: 47f7d9c6aa · **Ticket**: CASSANDRA-12856
**Subsystem**: Thrift transport (CustomTThreadPoolServer)
**Category**: Race condition / missing synchronization

---

## Root Cause
`CustomTThreadPoolServer` initialized `stopped = false` inside `serve()`, not in the constructor. If `stop()` was called before `serve()` ran (e.g., during a rapid shutdown sequence), `stopped` would be `true` at object creation time but then reset to `false` at the top of `serve()`, causing the server to enter its `while (!stopped)` accept loop and spin forever. Moving `stopped = false` into the constructor and removing the reset from `serve()` ensures that a pre-`serve()` call to `stop()` is respected.

### Before / After
```java
// BEFORE — stopped reset inside serve(), racing with stop()
private final AtomicInteger activeClients = new AtomicInteger(0);

public void serve() {
    ...
    stopped = false;        // ← overwrites any prior stop() call
    while (!stopped) { ... }
}

// AFTER — stopped and activeClients initialized in constructor
public CustomTThreadPoolServer(TThreadPoolServer.Args args, ExecutorService executorService) {
    super(args);
    this.executorService = executorService;
    this.stopped = false;
    this.args = args;
    this.activeClients = new AtomicInteger(0);
}

public void serve() {
    ...
    // stopped = false; ← removed
    while (!stopped) { ... }
}
```

---

## How to Spot
Any mutable flag that controls a loop or lifecycle state machine should be set to its initial value in the constructor, not at the beginning of the method that reads it. Setting it inside the method creates a TOCTOU window: any `stop()`/`close()` call that arrives between object construction and method entry will be silently overwritten.

---

## Test
No test added.
