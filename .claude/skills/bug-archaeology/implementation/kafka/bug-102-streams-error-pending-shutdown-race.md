**Commit**: 102de21355 · **Ticket**: KAFKA-17379
**Subsystem**: streams / KafkaStreams
**Category**: Race condition / missing synchronization

**Root cause**
`KafkaStreams.close()` checked the current state with two non-atomic reads: first with `isShuttingDown()` and then with `setState(PENDING_SHUTDOWN)`. A concurrent thread could transition from `PENDING_ERROR` to `ERROR` in the gap, leaving the closing thread seeing a state it did not expect and throwing a `StreamsException` ("Failed to shut down while in state ERROR") even though `ERROR` is a terminal, acceptable state to close from. The fix first attempts `setState(PENDING_SHUTDOWN)` and treats all shutdown-family states as valid non-error outcomes.

**Before/After**
```java
// Before — two separate checks, window for a race
if (state.isShuttingDown()) { ... }
if (!setState(State.PENDING_SHUTDOWN)) {
    log.error("Failed to transition to PENDING_SHUTDOWN, current state is {}", state);
    throw new StreamsException("Failed to shut down while in state " + state);
}

// After — single CAS-like attempt first
if (!setState(State.PENDING_SHUTDOWN)) {
    final State immutableStateCopy = state;
    if (immutableStateCopy.isShuttingDown()) { /* wait */ }
    if (state.hasCompletedShutdown()) { return true; }
    throw new IllegalStateException("unexpected");
}
```

**How to spot**
Any place that reads a volatile/mutable state variable twice in an if-chain without holding a lock creates a check-then-act race. Kafka Streams' state machine transitions are driven by multiple threads (stream threads + application thread), so every multi-read sequence around state needs synchronization.

**Test**
`KafkaStreamsTest` — exercises concurrent `close()` calls where one thread is already in `PENDING_ERROR` / `ERROR`; asserts no `StreamsException` is thrown and the final state is either `NOT_RUNNING` or `ERROR`.
