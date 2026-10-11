**Commit**: 32c887b05e · **Ticket**: KAFKA-17949
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This PR introduces the unified GroupState enum for all group types from KIP-1043. This PR also removes ShareGroupState and begins the work to replace Admin.listShareGroups with Admin.listGroups. That will complete in a future PR. Reviewers: Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
    private final ConsumerGroupState state;
        this.state = state;
            state == that.state &&

// After
import org.apache.kafka.common.GroupState;
    private final GroupState groupState;
    /**
     * @deprecated Since 4.0. Use {@link #ConsumerGroupDescription(String, boolean, Collection, String, GroupState, Node)}.
     */
    @Deprecated
    /**
     * @deprecated Since 4.0. Use {@link #ConsumerGroupDescription(String, boolean, Collection, String, GroupState, Node, Set)}.
     */
    @Deprecated
    /**
     * @deprecated Since 4.0. Use {@link #ConsumerGroupDescription(String, boolean, Collection, String, GroupType, GroupState, Node, Set)}.
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17949: introduce groupstate and replace sharegroupstate (#17763).

**Test**
Test modified in GroupListingTest.java covering the corrected behaviour.
