**Commit**: 9a239c6142 · **Ticket**: KAFKA-16955
**Subsystem**: streams/src
**Category**: Race condition / missing synchronization

**Root cause**
Each KafkaStreams instance maintains a map from threadId to state
to use to aggregate to a KafkaStreams app state. The map is updated
on every state change, and when a new thread is created. State change
updates are done in a synchronized blocks, however the update that
happens on thread creation is not, which can raise
ConcurrentModificationException. This patch moves this update
into the listener object and protects it using the object's lock.

**Before/After**
```java
// Before
    private final Map<Long, StreamThread.State> threadState;
    final class StreamStateListener implements StreamThread.StateListener {
        // this lock should always be held before the state lock
        private final Object threadStatesLock;
        StreamStateListener(final Map<Long, StreamThread.State> threadState,
                            final GlobalStreamThread.State globalThreadState) {
            this.threadState = threadState;
            this.threadStatesLock = new Object();
            synchronized (threadStatesLock) {
                // StreamThreads first
                if (thread instanceof StreamThread) {
                    final StreamThread.State newState = (StreamThread.State) abstractNewState;

// After
    private final class StreamStateListener implements StreamThread.StateListener {
        StreamStateListener(final GlobalStreamThread.State globalThreadState) {
            this.threadState = new HashMap<>();
            // StreamThreads first
            if (thread instanceof StreamThread) {
                final StreamThread.State newState = (StreamThread.State) abstractNewState;
                threadState.put(thread.getId(), newState);

                if (newState == StreamThread.State.PARTITIONS_REVOKED || newState == StreamThread.State.PARTITIONS_ASSIGNED) {
                    setState(State.REBALANCING);
                } else if (newState == StreamThread.State.RUNNING) {
                    maybeSetRunning();
```

**How to spot**
When reviewing streams/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16955: fix synchronization of streams threadstate (#16337).

**Test**
No test added.
