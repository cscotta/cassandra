## Commit

e70d05ecda — KAFKA-19217: Fix flaky ShareConsumerTest assertion and missing offset-reset setup

## Ticket

KAFKA-19217

## Subsystem

Share groups / consumer tests

## Category

Logic error in condition

## Root cause

`ShareConsumerTest` contained the assertion `assertTrue(producerCount < consumerCount)`, but the intent was to verify that the consumer received at least 95% of the produced records across a multi-threaded run (three consumer threads vs one producer). The wrong comparison direction meant the test passed trivially whenever any record was consumed, not when throughput reached the expected threshold. Additionally, the test did not call `alterShareAutoOffsetReset` before starting the consumer, so the consumer could miss records produced before the share session was established.

## Before / After

```java
// Before — wrong comparison, always trivially true
assertTrue(producerCount < consumerCount);

// After — correct threshold assertion
assertTrue(consumerCount >= 3 * producerCount * 0.95);

// Before — no offset-reset setup
consumer.subscribe(Collections.singleton(topicName));

// After — reset to earliest before subscribing
alterShareAutoOffsetReset(cluster, groupId, "earliest");
consumer.subscribe(Collections.singleton(topicName));
```

## How to spot

In throughput or fairness tests, verify that the assertion direction matches the intended invariant. A test that checks `produced < consumed` rather than `consumed >= threshold * produced` passes vacuously and provides no protection against regressions.

## Test

The corrected test in `ShareConsumerTest` now reliably fails when consumer throughput drops below 95% of producer throughput.
