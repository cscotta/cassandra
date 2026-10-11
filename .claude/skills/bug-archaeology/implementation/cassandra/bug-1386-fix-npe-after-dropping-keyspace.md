# Bug 1386: Fix NPE after dropping a keyspace

**Commit**: ac9cfbd9ae · **Ticket**: CASSANDRA-8332
**Subsystem**: io/compress/CompressionParameters.java
**Category**: Missing null / bounds check

---

## Root Cause

`CompressionParameters.setLiveMetadata()` was called with a `null` argument after a keyspace was dropped, because the compressor infrastructure still held a reference to the `CompressionParameters` object and attempted to update its live metadata pointer during teardown. The method had an assertion `this.liveMetadata == null || this.liveMetadata == liveMetadata` but no null guard on the incoming argument, so passing `null` caused a NullPointerException on the assertion or on any subsequent use of the reference.

The structural gap is that the method was designed for the normal lifecycle (initial attachment of metadata) without considering the cleanup path where `null` signals detachment.

### Before / After

```java
// BEFORE
public void setLiveMetadata(final CFMetaData liveMetadata)
{
    assert this.liveMetadata == null || this.liveMetadata == liveMetadata;
    this.liveMetadata = liveMetadata;
}
```

```java
// AFTER
public void setLiveMetadata(final CFMetaData liveMetadata)
{
    if (liveMetadata == null)
        return;

    assert this.liveMetadata == null || this.liveMetadata == liveMetadata;
    this.liveMetadata = liveMetadata;
}
```

---

## How to Spot

Setter methods with non-null assertions but no null guard on the incoming parameter are vulnerable whenever callers operate during shutdown or resource teardown sequences. Review all `set*` methods that hold object references for the presence of an early null check, especially in classes that participate in lifecycle management tied to keyspace or table events.

---

## Test

No test added.
