## Commit

- Hash: 4fcde4542b
- Tickets: KAFKA-18469, KAFKA-18036
- Subject: KAFKA-18469: Trigger metadata update when ListOffsets request is retried

## Subsystem

clients / async consumer / OffsetsRequestManager

## Category

Missing null / bounds check

## Root cause

When a `ListOffsets` request was retried due to a retriable error (e.g. leader not available), `OffsetsRequestManager` never called `metadata.requestUpdate(false)` before retrying. Because the metadata remained stale, the retry would immediately go to the same unavailable leader, loop indefinitely, and the consumer would stall without making progress.

## Before

```java
// No metadata refresh triggered on retry — request recycled as-is
requestsToRetry.add(request);
```

## After

```java
// Trigger non-forced metadata refresh so the retry picks up updated leader info
metadata.requestUpdate(false);
requestsToRetry.add(request);
```

## How to spot

Look for retry loops in async-consumer offset-fetching code where retriable errors silently recycle the request without refreshing cluster metadata. Any place that calls `requestsToRetry.add(...)` without a preceding `metadata.requestUpdate(...)` is suspect.

## Test

Added `testMetadataUpdateOnRetriableError` unit test in `OffsetsRequestManagerTest` verifying that `metadata.requestUpdate(false)` is invoked when the `ListOffsets` response returns a retriable error code.
