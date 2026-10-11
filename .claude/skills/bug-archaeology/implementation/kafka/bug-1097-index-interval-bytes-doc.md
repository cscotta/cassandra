## Commit

1ccaddaa70 — KAFKA-19209: Clarify index.interval.bytes impact on time index entries

## Ticket

KAFKA-19209

## Subsystem

Documentation / log storage configuration

## Category

Wrong constant / default value

## Root cause

The Javadoc for `TopicConfig.INDEX_INTERVAL_BYTES_DOC` described how `index.interval.bytes` controls the frequency of offset index entries but omitted the fact that time index entries are also written at the same interval, with the caveat that a time index entry is only written if the batch timestamp has advanced since the last time-index entry. Without this clarification, operators tuning `index.interval.bytes` for time-based log compaction or time-based offset lookups would not understand why time index entry counts differ from offset index entry counts at the same interval setting.

## Before / After

```java
// Before — mentions offset index only
"The interval with which an offset index entry is added to the offset index."

// After — mentions both indexes and the conditional time-index entry
"The interval with which an offset index entry is added to the offset index.
 At the same interval, a time index entry is conditionally added to the time
 index if the batch timestamp is greater than the last time index entry."
```

## How to spot

Search for configuration documentation that mentions only `offset index` without also noting the correlated `time index` behavior. The two indexes share the same `index.interval.bytes` trigger but have different entry-addition conditions; documentation that omits this correlation misleads operators tuning time-based retention.

## Test

Documentation-only change; no automated test added.
