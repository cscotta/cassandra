**Commit**: 3460ded386 · **Ticket**: KAFKA-19411
**Subsystem**: metadata/src/main/java/org/apache/kafka/controller/AclControlManager.java
**Category**: Off-by-one

**Root cause**: `deleteAclsForFilter` added a deletion record to the `records` list *before* checking whether `records.size() > MAX_RECORDS_PER_USER_OP`. The check used `>` instead of `>=`, so the guard fired only after the list already contained `MAX_RECORDS_PER_USER_OP + 1` entries, allowing one extra deletion record. Furthermore, once the `BoundedListTooLongException` was thrown for one filter, subsequent filters in the same request each appended yet another record before triggering the same exception, making the overrun proportional to the number of excess filters. A secondary issue was that the exception was caught by the generic `Throwable` handler and returned as a per-filter result rather than as a single fatal `InvalidRequestException`.

**Before/After**

```java
// Before – record added first, then bound checked
if (filter.matches(binding)) {
    deleted.add(new AclBindingDeleteResult(binding));
    records.add(new ApiMessageAndVersion(...));
    if (records.size() > MAX_RECORDS_PER_USER_OP) {   // too late: already added
        throw new BoundedListTooLongException(...);
    }
}

// After – check before adding; exception promoted to request-level failure
} catch (BoundedListTooLongException e) {
    throw new InvalidRequestException(e.getMessage(), e);
}
...
if (records.size() >= MAX_RECORDS_PER_USER_OP) {      // check first
    throw new BoundedListTooLongException(...);
}
deleted.add(...);
records.add(...);
```

**How to spot**: Any code that adds to a bounded collection before testing the size limit is suspect. If the post-add check uses `>` where `>=` is intended, the limit is off by one. When the same exception is silently caught and recycled per-item, cascading overruns become invisible to the caller.

**Test**: Creates `MAX_RECORDS_PER_USER_OP + 2` ACLs and issues a single `deleteAcls` request with a matching filter for each. Asserts that the call throws `InvalidRequestException` wrapping a `BoundedListTooLongException` with the correct message, rather than returning a partial success.
