**Commit**: 82241e187a · **Ticket**: KAFKA-19891

**Subsystem**: group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java

**Category**: Logic error in condition

**Root cause**
When a consumer group member changed its regex subscription from a non-empty pattern to empty (i.e. unsubscribed from a regex), the `updateRegularExpressions` method had no branch for that transition and therefore never returned `REGEX_UPDATED_AND_RESOLVED`. As a result the coordinator did not trigger a re-evaluation of the subscription epoch, leaving the group stuck with a stale epoch that could cause partition assignment inconsistencies. The fix adds an `else if (isNotEmpty(oldSubscribedTopicRegex))` branch that returns the correct result for the non-empty-to-empty transition.

**Before**
```java
// Before: no branch for empty new regex when old regex was non-empty
if (isNotEmpty(newSubscribedTopicRegex)) {
    updateRegularExpressionsResult =
        UpdateRegularExpressionsResult.REGEX_UPDATED_AND_NEEDS_REFRESH;
}
// silent fall-through when old was non-empty and new is empty
```

**After**
```java
// After
if (isNotEmpty(newSubscribedTopicRegex)) {
    updateRegularExpressionsResult =
        UpdateRegularExpressionsResult.REGEX_UPDATED_AND_NEEDS_REFRESH;
} else if (isNotEmpty(oldSubscribedTopicRegex)) {
    updateRegularExpressionsResult =
        UpdateRegularExpressionsResult.REGEX_UPDATED_AND_RESOLVED;
}
```

**How to spot**
In `updateRegularExpressions` look for a conditional that handles the new-non-empty case but has no corresponding branch for old-non-empty/new-empty; any unhandled transition in a state-machine method that returns an enum result is likely to leave the caller in an incorrect state.

**Test**
Write a unit test that first subscribes with a regex, triggers resolution, then clears the regex, and asserts that the subscription epoch advances and `REGEX_UPDATED_AND_RESOLVED` is returned.
