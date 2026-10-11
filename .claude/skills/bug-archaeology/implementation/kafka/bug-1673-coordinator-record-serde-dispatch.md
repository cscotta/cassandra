## Commit

- Hash: 87334e6c2e
- Tickets: KAFKA-18308
- Subject: KAFKA-18308: Replace manual switch in GroupCoordinator/ShareCoordinator RecordSerde with generated dispatch

## Subsystem

group-coordinator / share-coordinator / CoordinatorRecordSerde

## Category

Silent skip on non-match

## Root cause

`GroupCoordinatorRecordSerde` and `ShareCoordinatorRecordSerde` each contained a 17-case `switch` on `recordType` to deserialize coordinator log records. When a new record type was introduced, developers had to manually add a case to every `Serde` class. A missing case caused an `UnknownServerException` or a silent null deserialization rather than a compile-time failure. The generated `CoordinatorRecordType` enum now carries factory methods, eliminating the manual maintenance.

## Before

```java
switch (recordType) {
    case 0: return new OffsetCommitKey(...);
    case 1: return new GroupMetadataKey(...);
    // 15 more cases — must be manually kept in sync
    default: throw new UnknownServerException(...);
}
```

## After

```java
return CoordinatorRecordType.fromId(recordType).newRecordKey(buffer, version);
```

## How to spot

Large switch blocks over protocol record type IDs in Serde/converter classes that mirror an exhaustive enum elsewhere. The enum should own the dispatch; the switch is a maintenance footgun.

## Test

Serde round-trip tests cover all 17 record types via the generated dispatch.
