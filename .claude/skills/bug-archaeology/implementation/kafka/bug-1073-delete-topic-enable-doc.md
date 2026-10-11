## Commit

1c3c361629 — KAFKA-19263: Javadoc for delete.topic.enable incorrectly states deletion succeeds silently

## Ticket

KAFKA-19263

## Subsystem

Documentation / broker configuration

## Category

Wrong constant / default value

## Root cause

The Javadoc for `delete.topic.enable` stated that if the configuration was set to `false`, a delete-topic request would succeed silently (the topic would not be deleted but no error would be returned). The actual broker behaviour is to throw an `InvalidRequestException` when `delete.topic.enable=false` and a deletion is attempted. Operators relying on the documented "silent skip" behaviour would write code that assumed success without checking for exceptions, leading to unexpected failures in production.

## Before / After

```java
// Before — incorrect documentation
/** If false, delete topic will succeed silently without deleting the topic. */

// After — accurate documentation
/** If false, delete topic will throw an exception rather than deleting. */
```

## How to spot

Search for configuration Javadoc that uses phrases like "succeed silently," "no-op," or "ignored" to describe what happens when a flag is disabled. Cross-reference the actual request-handler code to verify whether an exception is thrown or a silent skip occurs.

## Test

Documentation-only change; existing `DeleteTopicsRequestTest` verifies the exception behaviour.
