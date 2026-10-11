**Commit**: 3460ded386 · **Ticket**: KAFKA-19411
**Subsystem**: metadata/src/main/java/org/apache/kafka/controller/AclControlManager.java
**Category**: Off-by-one

**Root cause**: `deleteAclsForFilter` checked the record count *after* appending each new `RemoveAccessControlEntryRecord`. Because the check was `> MAX_RECORDS_PER_USER_OP` (strictly greater), it allowed exactly `MAX_RECORDS_PER_USER_OP + 1` records to be added before throwing `BoundedListTooLongException`. Additionally, the exception was caught in the per-filter loop and turned into a per-filter error result, so partial deletions could silently succeed even when the limit was exceeded. The fix (a) changes the guard to `>= MAX_RECORDS_PER_USER_OP` (checked *before* the append), and (b) re-throws `BoundedListTooLongException` as `InvalidRequestException` at the outer loop level, aborting the entire operation.

**Before/After**:
```java
// BEFORE – off-by-one: check after append, catches exception per filter
deleted.add(new AclBindingDeleteResult(binding));
records.add(new ApiMessageAndVersion(...));
if (records.size() > MAX_RECORDS_PER_USER_OP) {
    throw new BoundedListTooLongException(...);
}
// outer loop catches Throwable and adds partial result

// AFTER – check before append, rethrows to abort whole operation
if (records.size() >= MAX_RECORDS_PER_USER_OP) {
    throw new BoundedListTooLongException(...);
}
deleted.add(new AclBindingDeleteResult(binding));
records.add(new ApiMessageAndVersion(...));
// outer loop catches BoundedListTooLongException separately and rethrows
```

**How to spot**: When a size-limit guard uses `>` after an unconditional `list.add(...)`, the effective limit is `MAX + 1`. Additionally, catching `Throwable` in a per-element loop and converting exceptions to partial results can mask limit violations — check that limit exceptions are propagated to the caller.

**Test**: `AclControlManagerTest.testDeleteExceedsMaxRecords` creates `MAX_RECORDS_PER_USER_OP + 2` ACLs and attempts to delete them all with individual filters. It asserts `InvalidRequestException` is thrown with `BoundedListTooLongException` as the cause and the exact limit message.
