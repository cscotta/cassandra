**Commit**: d646a09dd0 · **Ticket**: KAFKA-16531
**Subsystem**: raft/src
**Category**: Logic error in condition

**Root cause**
In the check-quorum calculation, the leader should not assume that it is part of the voter set. This may happen when the leader is removing itself from the voter set. This PR improves it by checking if leader is in the voter set or not, and decide how many follower fetches required. Also add tests.

**Before/After**
```java
// Before
                "Did not receive fetch request from the majority of the voters within {}ms. Current fetched voters are {}.",
                fetchedVoters);
        // The majority number of the voters excluding the leader. Ex: 3 voters, the value will be 1
        int majority = voterStates.size() / 2;

// After
                "Did not receive fetch request from the majority of the voters within {}ms. Current fetched voters are {}, and voters are {}",
                fetchedVoters,
                voterStates.keySet());
        // The majority number of the voters. Ex: 2 for 3 voters, 3 for 4 voters... etc.
        int majority = (voterStates.size() / 2) + 1;
        // If the leader is in the voter set, it should be implicitly counted as part of the
        // majority, but the leader will never be a member of the fetchedVoters.
        // If the leader is not in the voter set, it is not in the majority. Then, the
        // majority can only be composed of fetched voters.
        if (voterStates.containsKey(localId)) {
            majority = majority - 1;
        }
```

**How to spot**
When reviewing raft/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16531: calculate check-quorum when leader is not in voter set (#16211).

**Test**
Test modified in LeaderStateTest.java covering the corrected behaviour.
