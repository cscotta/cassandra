# Bug 0173: Credential refresh stops after one cycle — next refresh not scheduled

**Commit**: 7480c3bc2be4c24750fbf39320f845e578a4f03b · **Ticket**: #15732

## Detection Rule

When a periodic refresh task updates state and then exits, verify that the task reschedules itself (or an equivalent follow-up task) before returning; a one-shot scheduled task that does not re-enqueue itself will silently stop refreshing after the first cycle.

## Code Smell

A `ScheduledExecutorService.schedule()` call that triggers a method which updates shared state but does not call `scheduleCredentialRefresh()` again, so the next expiry window is never registered.

## Tags

- scope: universal
- category: state-not-cleaned
- code-pattern: missing-self-reschedule
- severity: silent-wrong-result
- subsystem: aws/s3/S3FileIO

## Root Cause

`refreshStorageCredentials()` was introduced to fire once when credentials were near expiry, update `storageCredentials`, and return. It never called `scheduleCredentialRefresh()` after storing the new credentials, so if the freshly-obtained credentials themselves had an expiry the refresh cycle would not continue. The fix adds a single call to `scheduleCredentialRefresh()` after the updated credentials are stored, re-arming the timer based on the new expiry time.

## Example

```java
// BEFORE
if (!refreshed.isEmpty() && !isResourceClosed.get()) {
  this.storageCredentials = Lists.newArrayList(refreshed);
  // next refresh never scheduled — cycle ends here
}
```

```java
// AFTER
if (!refreshed.isEmpty() && !isResourceClosed.get()) {
  this.storageCredentials = Lists.newArrayList(refreshed);
  scheduleCredentialRefresh(); // re-arm timer based on new expiry
}
```
