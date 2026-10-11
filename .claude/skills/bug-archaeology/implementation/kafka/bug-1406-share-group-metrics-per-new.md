**Commit**: c89fd2bff6 · **Ticket**: KAFKA-18828
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
* Due to recent changes in the way group count metrics are initialized
and updated, the current share group count code has become obsolete as
well as non-functional. * The update method for the share group count which should be called
from `ShareGroup` cannot be called either. This is because the
constructor has been changed to NOT accept the
`GroupCoordinatorShardMetrics` ref. * In this PR, we remedy the situation by bringing share group count code
at par with consumer and streams groups.

**Before/After**
```java
// Before
    public static final String SHARE_GROUP_COUNT_METRIC_NAME = "group-count";
            SHARE_GROUP_COUNT_METRIC_NAME,
            Collections.singletonMap(SHARE_GROUP_PROTOCOL_TAG, Group.GroupType.SHARE.toString())
            SHARE_GROUP_PROTOCOL_TAG, Group.GroupType.SHARE.toString(),
            SHARE_GROUP_COUNT_STATE_TAG, ShareGroup.ShareGroupState.EMPTY.toString()
            SHARE_GROUP_PROTOCOL_TAG, Group.GroupType.SHARE.toString(),
            SHARE_GROUP_COUNT_STATE_TAG, ShareGroup.ShareGroupState.STABLE.toString()
            SHARE_GROUP_PROTOCOL_TAG, Group.GroupType.SHARE.toString(),
            SHARE_GROUP_COUNT_STATE_TAG, ShareGroup.ShareGroupState.DEAD.toString()

// After
        Map<ShareGroup.ShareGroupState, Long> shareGroupSizeCounter = new HashMap<>();
                case SHARE:
                    shareGroupSizeCounter.compute(((ShareGroup) group).state(), Utils::incValue);
                    break;
        metrics.setShareGroupGauges(shareGroupSizeCounter);
    public static final String SHARE_GROUP_COUNT_METRIC_NAME = "share-group-count";
            GROUP_COUNT_METRIC_NAME,
            Map.of(SHARE_GROUP_PROTOCOL_TAG, Group.GroupType.SHARE.toString())
            Map.of(SHARE_GROUP_COUNT_STATE_TAG, ShareGroup.ShareGroupState.EMPTY.toString())
            Map.of(SHARE_GROUP_COUNT_STATE_TAG, ShareGroup.ShareGroupState.STABLE.toString())
            Map.of(SHARE_GROUP_COUNT_STATE_TAG, ShareGroup.ShareGroupState.DEAD.toString())
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18828: update share group metrics per new init and call mechanism. (#18962).

**Test**
Test modified in GroupCoordinatorMetricsTest.java covering the corrected behaviour.
