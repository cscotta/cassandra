**Commit**: 865cdfc1cd · **Ticket**: KAFKA-12834
**Subsystem**: streams/test-utils/MockProcessorContext
**Category**: Wrong constant / default value

**Root cause**
`MockProcessorContext.setTimestamp(long)` was deprecated in 3.0.0 in favour of `setRecordTimestamp(long)`. The internal error message in `timestamp()` still referenced the old method name `setTimestamp()` in its exception text, which would mislead users who had already migrated to the new API and were confused about which method to call. Removing `setTimestamp` and updating the message to reference `setRecordTimestamp` provides accurate guidance.

**Before/After**
```java
// Before
@Deprecated
public void setTimestamp(final long timestamp) {
    this.recordTimestamp = timestamp;
}
// error message
throw new IllegalStateException("...via setRecordMetadata() or setTimestamp().");

// After
// setTimestamp removed
throw new IllegalStateException("...via setRecordMetadata() or setRecordTimestamp().");
```

**How to spot**
After removing a deprecated method, search the same class for any exception messages, JavaDoc, or comments that still reference the removed method's name. A stale method name in an error message is a silent usability bug that misdirects developers at runtime.

**Test**
No test added.
