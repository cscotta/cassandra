## Commit

54fd1361e5 — KAFKA-19264: Remove stale fallback for remote log manager thread pool sizes

## Ticket

KAFKA-19264

## Subsystem

Storage / remote log management configuration

## Category

Wrong constant / default value

## Root cause

`RemoteLogManagerConfig.remoteLogManagerCopierThreadPoolSize()` and `remoteLogManagerExpirationThreadPoolSize()` contained a fallback: if the per-pool config was set to `-1`, the value of the legacy `remote.log.manager.thread.pool.size` was used instead. This fallback was introduced by KIP-950 and was supposed to be a migration aid. KIP-1030 changed the defaults from `-1` to `10` and added a validator requiring a minimum of `1`, making it impossible to trigger the fallback in any valid configuration. The dead fallback code was confusing and could mislead operators into thinking `-1` was a valid sentinel value when it would now be rejected by validation.

## Before / After

```java
// Before — dead fallback path, -1 rejected by validator anyway
public int remoteLogManagerCopierThreadPoolSize() {
    int size = config.getInt(REMOTE_LOG_MANAGER_COPIER_THREAD_POOL_SIZE_PROP);
    return size == -1 ? remoteLogManagerThreadPoolSize() : size;
}

// After — direct read, validator ensures value is always >= 1
public int remoteLogManagerCopierThreadPoolSize() {
    return config.getInt(REMOTE_LOG_MANAGER_COPIER_THREAD_POOL_SIZE_PROP);
}
```

## How to spot

Search for sentinel-value fallback patterns (`if (value == -1) return otherConfig`) in configuration accessors. After a validator raises the minimum above the sentinel, the fallback becomes unreachable dead code that should be removed to avoid confusion.

## Test

`RemoteLogManagerConfigTest` was updated to assert that the direct accessor returns the configured value without any `-1` fallback.
