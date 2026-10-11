**Commit**: a85f0a89c8 · **Ticket**: KAFKA-17112
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
            // KAFKA-17122 manually stop taskManager if `thread` is not in CREATED state.
            if (thread.state() != State.CREATED) {
                thread.taskManager().shutdown(false);
            }
        final Set<Thread> t = Collections.unmodifiableSet(Thread.getAllStackTraces().keySet());
        assertTrue(t.stream().noneMatch(thread -> thread.getName().contains("TaskExecutor")));
        assertTrue(t.stream().noneMatch(thread -> thread.getName().contains("StateUpdater")));
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17112: manually stop task manager if stream thread is not in created state (#16625).

**Test**
Test modified in StreamThreadTest.java covering the corrected behaviour.
