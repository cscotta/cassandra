**Commit**: 2e6e5304c0 · **Ticket**: KAFKA-18168
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
To address the issue of not creating a checkpoint file during the
restoring and closing process, called the
GlobalStateUpdateTask.flushState() method in
GlobalStateUpdateTask.initialize() and GlobalStateUpdateTask.close()
methods. This will flush the state and create a checkpoint file thereby,
avoiding the need to completely restore the entire state. Reviewers: Alieh Saeedi <asaeedi@confluent.io>, Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
        flushState();
        if (!wipeStateStore) {
            flushState();
        }

        stateMgr.checkpointWritten = false;
        stateMgr.flushed = false;


        stateMgr.checkpointWritten = false;
        stateMgr.flushed = false;

```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18168: adding checkpointing for globalktable during restoration and closing (#18752).

**Test**
Test modified in GlobalStateTaskTest.java covering the corrected behaviour.
