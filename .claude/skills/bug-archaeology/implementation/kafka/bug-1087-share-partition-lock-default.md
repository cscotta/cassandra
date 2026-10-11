## Commit

b4b73c604b — KAFKA-19245: SHARE_GROUP_PARTITION_MAX_RECORD_LOCKS_DEFAULT too low for concurrent fetch

## Ticket

KAFKA-19245

## Subsystem

Share groups / configuration defaults

## Category

Wrong constant / default value

## Root cause

`ShareGroupConfig.SHARE_GROUP_PARTITION_MAX_RECORD_LOCKS_DEFAULT` was set to 200. With the default `maxFetchRecords` of 500, a single fetch could request more records than the maximum allowed lock count for a partition, making it impossible for two consumers to fetch from the same partition in parallel: the first consumer would hold 200 locks and the second would be blocked from acquiring any. This default was set during early development before the fetch-record default was raised to 500, and was never updated to keep pace. The fix raises the default to 2000, allowing at least four parallel consumers each holding 500-record batches.

## Before / After

```java
// Before — too low for default maxFetchRecords=500
public static final int SHARE_GROUP_PARTITION_MAX_RECORD_LOCKS_DEFAULT = 200;

// After — consistent with maxFetchRecords default
public static final int SHARE_GROUP_PARTITION_MAX_RECORD_LOCKS_DEFAULT = 2000;
```

## How to spot

When a resource-limit default is set independently of a related capacity default, they will drift apart. Search for `MAX_RECORD_LOCKS_DEFAULT` and compare it against `maxFetchRecords`. If the lock limit is less than the fetch batch size, parallel consumers will be mutually blocked.

## Test

`ShareGroupConfigTest` was updated to assert the new default value and verify that a single consumer can acquire 500 locks without hitting the partition limit.
