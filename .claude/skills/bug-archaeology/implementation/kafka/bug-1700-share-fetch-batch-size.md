## Commit

- Hash: 3f9d2c2db0
- Tickets: KAFKA-18433
- Subject: KAFKA-18433: Add BatchSize field to ShareFetch request (1/N)

## Subsystem

clients / share groups / ShareFetchRequest

## Category

Wrong constant / default value

## Root cause

`ShareFetchRequest` lacked a `BatchSize` field to communicate the optimal number of records per acknowledgement batch to the broker. Without it, the broker had no guidance on batch sizing and used an internal default that could produce batches much larger or smaller than the consumer's `max.poll.records` setting, causing either excessive memory pressure (oversized batches) or poor throughput (many tiny batches). The field carries `fetchConfig.maxPollRecords` from the consumer configuration.

## Before

```java
ShareFetchRequest.Builder.forConsumer(groupId, nextMetadata, fetchConfig.maxWaitMs,
    fetchConfig.minBytes, fetchConfig.maxBytes, fetchConfig.fetchSize,
    added, removed, acknowledgementBatches);
// No BatchSize in the request wire format
```

## After

```java
ShareFetchRequest.Builder.forConsumer(groupId, nextMetadata, fetchConfig.maxWaitMs,
    fetchConfig.minBytes, fetchConfig.maxBytes, fetchConfig.fetchSize,
    fetchConfig.maxPollRecords,  // batchSize added
    added, removed, acknowledgementBatches);
// ShareFetchRequest.json: new BatchSize int32 field
data.setBatchSize(batchSize);
```

Also: `PartitionMaxBytes` field in the JSON schema marked "TO BE REMOVED" in anticipation of the follow-on commits.

## How to spot

Request builder `forConsumer` / `forReplica` factory methods whose parameter list diverges from the full set of consumer fetch configuration fields — a missing parameter means some configuration is silently ignored on the wire.

## Test

`ShareFetchAcknowledgeRequestTest` updated to pass `batchSize` parameter; wire-format encoding verified.
