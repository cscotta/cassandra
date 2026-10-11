**Commit**: a12152f028 · **Ticket**: KAFKA-18263
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
When a static member rejoins the group, the group state is rewritten to the partition in order to persist the change. If the write fails, the change is reverted. However, this is done without acquiring the group lock. This is only try in the old group coordinator.

**Before/After**
```java
// Before
            if (error != Errors.NONE) {
              warn(s"Failed to persist metadata for group ${group.groupId}: ${error.message}")

              // Failed to persist member.id of the given static member, revert the update of the static member in the group.
              group.updateMember(knownStaticMember, oldProtocols, oldRebalanceTimeoutMs, oldSessionTimeoutMs, null)
              val oldMember = group.replaceStaticMember(groupInstanceId, newMemberId, oldMemberId)
              completeAndScheduleNextHeartbeatExpiration(group, oldMember)
              responseCallback(JoinGroupResult(
                List.empty,
                memberId = JoinGroupRequest.UNKNOWN_MEMBER_ID,
                generationId = group.generationId,
                protocolType = group.protocolType,

// After
            group.inLock {
              if (error != Errors.NONE) {
                warn(s"Failed to persist metadata for group ${group.groupId}: ${error.message}")

                // Failed to persist member.id of the given static member, revert the update of the static member in the group.
                group.updateMember(knownStaticMember, oldProtocols, oldRebalanceTimeoutMs, oldSessionTimeoutMs, null)
                val oldMember = group.replaceStaticMember(groupInstanceId, newMemberId, oldMemberId)
                completeAndScheduleNextHeartbeatExpiration(group, oldMember)
                responseCallback(JoinGroupResult(
                  List.empty,
                  memberId = JoinGroupRequest.UNKNOWN_MEMBER_ID,
                  generationId = group.generationId,
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18263; group lock must be acquired when reverting static membership rejoin (#18207).

**Test**
No test added.
