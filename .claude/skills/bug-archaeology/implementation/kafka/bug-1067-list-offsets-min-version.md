## Commit

f55565d321 — KAFKA-19267: ListOffsetsRequest.minVersion hardcoded to 0 instead of oldestVersion()

## Ticket

KAFKA-19267

## Subsystem

Clients / ListOffsets API versioning

## Category

Wrong constant / default value

## Root cause

`ListOffsetsRequest.Builder` set `minVersion = 0` as a hard-coded literal. The actual oldest supported version of the `LIST_OFFSETS` API is 1 (version 0 was retired). Using 0 as the minimum allowed the builder to construct version-0 requests that the broker would reject with `UNSUPPORTED_VERSION`. Brokers running in mixed-version clusters or with older compatibility settings received spurious errors on legitimate offset-listing operations. The fix replaces the literal with `ApiKeys.LIST_OFFSETS.oldestVersion()` so that future version floor changes are automatically reflected.

## Before / After

```java
// Before — hardcoded 0, below the actual oldest supported version
public Builder(Map<String, PartitionData> targetTimes, boolean requireTimestamp, IsolationLevel isolationLevel) {
    super(ApiKeys.LIST_OFFSETS, (short) 0, ApiKeys.LIST_OFFSETS.latestVersion());
}

// After — uses the API's actual oldest version
super(ApiKeys.LIST_OFFSETS,
      ApiKeys.LIST_OFFSETS.oldestVersion(),
      ApiKeys.LIST_OFFSETS.latestVersion());
```

## How to spot

Search `ApiKeys.*` request `Builder` constructors for literal `(short) 0` as the minimum version argument. Any literal that doesn't match `ApiKeys.<NAME>.oldestVersion()` is a potential source of `UNSUPPORTED_VERSION` errors or dead code handling a version that no broker accepts.

## Test

`ListOffsetsRequestTest` was updated to assert that the builder's minimum version equals `ApiKeys.LIST_OFFSETS.oldestVersion()`.
