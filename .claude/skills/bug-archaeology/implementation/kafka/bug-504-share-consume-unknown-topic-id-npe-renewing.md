## Bug 504 — ShareConsumeRequestManager: UNKNOWN_TOPIC_ID not treated as immediate failure; NPE on null renewingRecords

**Commit:** 6057c2d8c2
**Ticket:** (no JIRA in commit – share-consumer fix)
**Subsystem:** clients / share-consumer
**Category:** Missing null/bounds check; Logic error in condition

### Root cause
Two related defects in `ShareConsumeRequestManager` and `ShareInFlightBatch`:

1. **Wrong error check**: The error-handling branch checked only for `UNKNOWN_TOPIC_OR_PARTITION` as a terminal failure code. `UNKNOWN_TOPIC_ID` was silently skipped, causing the client to loop retrying a fetch for a topic UUID the broker does not recognise, rather than propagating the error immediately to the application.

2. **NPE on null renewingRecords**: `ShareInFlightBatch.renewingRecords` could be `null` when no records were in the renewing state. Code that iterated over it without a null check threw a `NullPointerException`.

### Before
```java
if (error == Errors.UNKNOWN_TOPIC_OR_PARTITION) {
    handleTopicNotFound(batch);
}
// UNKNOWN_TOPIC_ID not handled — silently ignored
```
```java
for (int offset : renewingRecords) { … }  // NPE if renewingRecords == null
```

### After
```java
if (error == Errors.UNKNOWN_TOPIC_OR_PARTITION
        || error == Errors.UNKNOWN_TOPIC_ID) {
    handleTopicNotFound(batch);
}
```
```java
if (renewingRecords != null) {
    for (int offset : renewingRecords) { … }
}
```

### How to spot
Error-code switch/if chains that list `UNKNOWN_TOPIC_OR_PARTITION` but omit `UNKNOWN_TOPIC_ID` (or vice-versa) are a recurring pattern. Similarly, lazy-initialised collections that are iterated without a null check are NPE candidates.

### Test
`ShareConsumeRequestManagerTest` extended to cover both error codes and the null-renewingRecords path.
