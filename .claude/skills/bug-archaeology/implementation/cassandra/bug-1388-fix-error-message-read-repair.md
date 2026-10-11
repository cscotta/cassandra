# Bug 1388: Fix error message on read repair timeouts

**Commit**: 451c514a3a · **Ticket**: CASSANDRA-7947
**Subsystem**: service/StorageProxy.java
**Category**: Wrong constant / default value

---

## Root Cause

When a read repair timed out, `StorageProxy` re-threw a `ReadTimeoutException` sourced directly from the caught exception. The repair handler used `ConsistencyLevel.ALL` internally (it requests full data from every selected replica), so the re-thrown exception contained `CL.ALL` as the consistency level even when the original client request used a different consistency level such as `QUORUM`. The client therefore received a misleading timeout message indicating `ALL` replicas were needed.

A second problem existed for `ReadTimeoutException` thrown during the repair reads: it was not caught at all, propagating with the wrong CL before the fix added the appropriate catch block.

### Before / After

```java
// BEFORE — TimeoutException re-thrown with internal CL, no handler for ReadTimeoutException
catch (TimeoutException e)
{
    Tracing.trace("Timed out on digest mismatch retries");
    int blockFor = consistencyLevel.blockFor(Keyspace.open(command.getKeyspace()));
    throw new ReadTimeoutException(consistencyLevel, blockFor-1, blockFor, true);
}
```

```java
// AFTER — explicit catch for ReadTimeoutException with corrected CL; separate message for repair reads vs acks
catch (ReadTimeoutException e)
{
    if (Tracing.isTracing())
        Tracing.trace("Timed out waiting on digest mismatch repair requests");
    else
        logger.debug("Timed out waiting on digest mismatch repair requests");
    int blockFor = consistencyLevel.blockFor(Keyspace.open(command.getKeyspace()));
    throw new ReadTimeoutException(consistencyLevel, blockFor-1, blockFor, true);
}
```

---

## How to Spot

When an internal helper operates at a different (often stricter) consistency level than the user-visible operation, re-throwing the helper's exception directly leaks internal details to the client. Look for catch blocks that wrap or re-throw timeout or failure exceptions and verify the `ConsistencyLevel`, `blockFor`, and `received` fields are reconstructed from the *original* request's CL, not the internal one.

---

## Test

No test added.
