## Commit

- Hash: bb22eec478
- Tickets: KAFKA-17455
- Subject: KAFKA-17455: Cap poll timeout by node delay in NetworkClientUtils.awaitReady

## Subsystem

clients / NetworkClientUtils

## Category

Race condition / missing synchronization

## Root cause

`NetworkClientUtils.awaitReady()` called `client.poll(pollTimeout, now)` without checking whether the target node had an outstanding throttle delay or backoff period. If the node was throttled, `client.pollDelayMs(node, now)` returned a value smaller than `pollTimeout`, but `poll()` was still called with the full timeout, blocking for far longer than necessary. In pathological cases (very high throttle + tight producer linger) this caused a stuck producer that appeared unresponsive.

## Before

```java
client.poll(pollTimeout, attemptStartTime);
```

## After

```java
long nodeDelay = client.pollDelayMs(node, now);
client.poll(Math.min(pollTimeout, nodeDelay), attemptStartTime);
```

## How to spot

`NetworkClient.poll()` calls that pass a fixed timeout without first consulting `client.pollDelayMs()` for any node with a pending throttle or retry backoff — the caller can block past the moment the node becomes ready.

## Test

`NetworkClientUtilsTest` verifies that `awaitReady` respects the node delay and does not block for the full timeout when a shorter delay applies.
