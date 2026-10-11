**Commit**: bbf48ad677 · **Ticket**: KAFKA-19208
**Subsystem**: streams/src/main/java/org/apache/kafka/streams/kstream/internals/KStreamKTableJoinProcessor.java
**Category**: Logic error in condition

**Root cause**: In `shouldDropRecord()`, the early-return guard for left-joins with null-key records tested `record.key() == null` instead of `mappedKey == null`. The `keyMapper` is responsible for deriving the lookup key from the record; when a record has a non-null stream key but the mapper produces `null` (e.g. because the value carries no foreign key), the record should be dropped but wasn't, because the condition evaluated against the original stream key rather than the mapped key. Conversely, when the stream key was null but the mapper produced a non-null result, the record was incorrectly dropped. The fix replaces `record.key() == null` with `mappedKey == null` in the guard.

**Before/After**

```java
// Before – wrong variable tested
if (leftJoin && record.key() == null && record.value() != null) {
    return false;  // should drop: don't forward this record
}

// After – correct variable
if (leftJoin && mappedKey == null && record.value() != null) {
    return false;
}
```

**How to spot**: In foreign-key join processors, always check the *mapped* key (the result of `keyMapper.apply(...)`) rather than the original record key when deciding whether to skip a record. Confusing the two silently drops or passes records incorrectly.

**Test**: `shouldNotJoinOnNullKeyMapperValuesWithNullKeys` — pushes records whose key is null and whose value also yields a null mapped key; asserts no output is produced and the dropped-records metric increments to 4.0.
