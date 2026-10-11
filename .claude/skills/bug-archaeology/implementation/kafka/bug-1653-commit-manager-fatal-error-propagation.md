## Commit

- Hash: 70d6312a3a
- Tickets: KAFKA-18034
- Subject: KAFKA-18034: Propagate fatal coordinator error through background event handler

## Subsystem

clients / async consumer / CommitRequestManager, CoordinatorRequestManager

## Category

State not cleaned up

## Root cause

When the group coordinator returned a fatal error (e.g. `GROUP_AUTHORIZATION_FAILED`), `CommitRequestManager` silently swallowed it after one retry rather than storing it in `CoordinatorRequestManager` and propagating it to the application thread via `BackgroundEventHandler`. Subsequent commit calls would retry indefinitely against the same broken coordinator state instead of surfacing the fatal error.

## Before

Fatal coordinator errors were discarded; no stored error state; `BackgroundEventHandler` never notified.

## After

```java
// CoordinatorRequestManager now stores the fatal error
void maybeFailOnCoordinatorFatalError(Errors error) {
    if (isFatal(error)) {
        fatalError = Optional.of(error);
        backgroundEventHandler.add(new ErrorEvent(error.exception()));
    }
}
```

`CommitRequestManager` calls `maybeFailOnCoordinatorFatalError()` on each response and surfaces the stored error on the next `poll()`.

## How to spot

Async-consumer managers that call a coordinator but have no path to store and re-surface fatal errors — the caller loops forever on every `poll()` without delivering an exception to the application.

## Test

`CommitRequestManagerTest` and `CoordinatorRequestManagerTest` extended with fatal-error propagation scenarios.
