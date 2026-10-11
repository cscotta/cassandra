**Commit**: 5545d717c3 · **Ticket**: KAFKA-17633
**Subsystem**: core/tools (DumpLogSegments, ShareGroupStateMessageFormatter)
**Category**: Silent skip on non-match

**Root cause**
The `kafka-dump-log` tool had no formatter for `__share_group_state` topic records (`ShareSnapshotKey/Value`, `ShareUpdateKey/Value`). Attempting to dump that internal topic produced no output or an opaque byte dump. Additionally, the share snapshot record value version was hardcoded as `(short) 0` instead of using the constant `ShareCoordinator.SHARE_SNAPSHOT_RECORD_VALUE_VERSION`, making future version bumps invisible to the formatter.

**Before/After**
```java
// Before – no case for share-group state records
// DumpLogSegments fell through with no output

// After – dedicated formatter
case SHARE_SNAPSHOT_KEY_TYPE:
    return ShareCoordinatorRecordHelpers
        .deserializeShareSnapshotKey(keyBuffer).toString();
// version constant used
(short) ShareCoordinator.SHARE_SNAPSHOT_RECORD_VALUE_VERSION
```

**How to spot**
After adding a new internal topic or record type, verify that `kafka-dump-log` (DumpLogSegments) has a corresponding formatter entry. Missing entries leave operators unable to inspect the topic during incident triage.

**Test**
Unit tests added in `ShareGroupStateMessageFormatterTest`.
