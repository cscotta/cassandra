## Commit

199772adc5 — KAFKA-19141: Add topicId field to OffsetCommitValue (KIP-848)

## Ticket

KAFKA-19141

## Subsystem

Group coordinator / offset commit persistence

## Category

Missing null / bounds check

## Root cause

`OffsetCommitValue` (the Kafka internal record schema for committed offsets) stored offsets keyed by topic name only. When topics were deleted and recreated, the topic name was reused but pointed to a different topic, so old committed offsets could be mistakenly applied to the new topic. Adding a `topicId` (UUID) field as a tagged field at version 4 allows the coordinator to detect the name-reuse case and discard stale committed offsets whose `topicId` no longer matches the current topic, preventing incorrect offset application after topic recreation.

## Before / After

```json
// Before — OffsetCommitValue v3, name-only key
{ "name": "offset", "type": "int64" }

// After — OffsetCommitValue v4, adds topicId tagged field
{ "name": "topicId", "type": "uuid",
  "versions": "4+", "taggedVersions": "4+", "tag": 0,
  "default": "AAAAAAAAAAAAAAAAAAAAAA",
  "about": "The topic ID, or zero if not known." }
```

## How to spot

When an offset store is keyed only by topic name, topic deletion-and-recreation will silently match old committed offsets to a new topic. Check `OffsetCommitValue` schema versions for the presence of a `topicId` field; its absence is a correctness gap in environments where topics are recycled.

## Test

`GroupCoordinatorRecordHelpersTest` was extended to assert that version-4 records encode and decode `topicId` correctly.
