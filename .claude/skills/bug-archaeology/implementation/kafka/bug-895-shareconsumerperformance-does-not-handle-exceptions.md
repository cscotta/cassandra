**Commit**: e489682c45 · **Ticket**: KAFKA-19450

**Subsystem**: Tools / ShareConsumerPerformance

**Category**: Missing error handler / unimplemented code path

**Root cause**
`ShareConsumerPerformance.consumeMessagesForSingleShareConsumer` submitted consumer tasks to an `ExecutorService` via `submit()` but discarded the returned `Future`. If a consumer thread threw an exception, it was silently swallowed — the performance tool appeared to finish normally while producing incomplete or zero results. The fix stores each `Future` in a list and calls `future.get()` for every submitted task, propagating any thrown exceptions to the caller.

**Before/After**
```java
// Before: Future discarded; exceptions silently lost
executorService.submit(() -> {
    consumeMessages(...);
});

// After: Future stored and checked
List<Future<?>> futures = new ArrayList<>();
futures.add(executorService.submit(() -> {
    consumeMessages(...);
}));
for (Future<?> future : futures) {
    future.get(); // propagates ExecutionException if consumer thread threw
}
```

**How to spot**
Any `executorService.submit(...)` call whose return value is not stored is a silent exception sink. Search for unassigned `submit(` calls in performance tool and test code; they should always be followed by `future.get()` or a completion handler.

**Test**
Manual testing confirms that exceptions thrown inside consumer threads are now surfaced to the tool's main thread and result in a non-zero exit code with an informative stack trace.
