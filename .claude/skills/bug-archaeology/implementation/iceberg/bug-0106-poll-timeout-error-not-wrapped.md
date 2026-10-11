# Bug 0106: REST scan planning timeout surfaces as opaque internal exception

**Commit**: e1a2713d18ae62ae91512ebbde052d137c942861 · **Ticket**: #15863

## Detection Rule

When a polling loop uses a sentinel exception (`onlyRetryOn(SomeException.class)`) to signal "not done yet" and that loop is bounded by a retry count or timeout, verify that the sentinel exception is caught and re-thrown as a typed, user-visible exception after retries are exhausted — not left to propagate as an internal detail.

## Code Smell

A `Tasks.foreach(...).onlyRetryOn(InternalException.class).throwFailureWhenFinished().run(...)` block where `InternalException` is never caught outside the block, meaning exhaustion of retries surfaces the internal sentinel as the caller-visible error.

## Tags

- scope: universal
- category: wrong-serialization
- code-pattern: sentinel-not-handled
- severity: silent-wrong-result
- subsystem: core/rest-catalog

## Root Cause

`RESTTableScan` used an internal `NotCompleteException` as a retry sentinel inside a `Tasks` polling loop. When the loop exhausted either `MAX_RETRIES` or `MAX_WAIT_TIME_MS`, `throwFailureWhenFinished()` re-threw the last exception, which was a raw `NotCompleteException`. Callers received no indication that a timeout occurred — only an opaque internal class — making the error impossible to diagnose or handle programmatically. The fix wraps the escaped sentinel in a `RemotePlanTimeoutException` that carries the configured timeout and retry count.

## Example

```java
// BEFORE
Tasks.foreach(planId)
    .exponentialBackoff(MIN_SLEEP_MS, MAX_SLEEP_MS, MAX_WAIT_TIME_MS, SCALE_FACTOR)
    .retry(MAX_RETRIES)
    .onlyRetryOn(NotCompleteException.class)
    .throwFailureWhenFinished()
    .run(id -> {
        if (response.planStatus() == PlanStatus.SUBMITTED) {
            throw new NotCompleteException(); // escapes as-is on exhaustion
        }
        result.set(response);
    });
```

```java
// AFTER
try {
    Tasks.foreach(planId)
        .exponentialBackoff(MIN_SLEEP_MS, MAX_SLEEP_MS, maxWaitTimeMs, SCALE_FACTOR)
        .retry(MAX_RETRIES)
        .onlyRetryOn(NotCompleteException.class)
        .throwFailureWhenFinished()
        .run(id -> {
            if (response.planStatus() == PlanStatus.SUBMITTED) {
                throw new NotCompleteException();
            }
            result.set(response);
        });
} catch (NotCompleteException e) {
    throw new RemotePlanTimeoutException(
        "Remote scan planning did not complete within configured limits"
            + " (timeout=" + maxWaitTimeMs + " ms, maxRetries=" + MAX_RETRIES + ")", e);
}
```
