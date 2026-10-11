**Commit**: fecbfb8133 · **Ticket**: KAFKA-16950
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Define the interfaces and RPCs for share-group persistence. (KIP-932). This PR is just RPCs and interfaces to allow building of the broker components which depend upon them. The implementation will follow in subsequent PRs.

**Before/After**
```java
// Before
    UPDATE_RAFT_VOTER(ApiMessageType.UPDATE_RAFT_VOTER);

// After
    UPDATE_RAFT_VOTER(ApiMessageType.UPDATE_RAFT_VOTER),
    INITIALIZE_SHARE_GROUP_STATE(ApiMessageType.INITIALIZE_SHARE_GROUP_STATE, true),
    READ_SHARE_GROUP_STATE(ApiMessageType.READ_SHARE_GROUP_STATE, true),
    WRITE_SHARE_GROUP_STATE(ApiMessageType.WRITE_SHARE_GROUP_STATE, true),
    DELETE_SHARE_GROUP_STATE(ApiMessageType.DELETE_SHARE_GROUP_STATE, true),
    READ_SHARE_GROUP_STATE_SUMMARY(ApiMessageType.READ_SHARE_GROUP_STATE_SUMMARY, true);
            case INITIALIZE_SHARE_GROUP_STATE:
                return InitializeShareGroupStateRequest.parse(buffer, apiVersion);
            case READ_SHARE_GROUP_STATE:
                return ReadShareGroupStateRequest.parse(buffer, apiVersion);
            case WRITE_SHARE_GROUP_STATE:
                return WriteShareGroupStateRequest.parse(buffer, apiVersion);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16950: define persister interfaces and rpcs (#16335).

**Test**
Test modified in RequestResponseTest.java covering the corrected behaviour.
