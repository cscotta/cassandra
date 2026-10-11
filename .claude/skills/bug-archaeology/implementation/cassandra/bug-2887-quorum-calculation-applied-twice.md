# Bug 2887: QUORUM consistency level calculation applies the formula twice

**Commit**: dc33a2d9f7 · **Ticket**: CASSANDRA-1487

## Detection Rule
When a helper method is renamed or inlined, search all callers to ensure none still apply the same formula to the helper's already-computed result.

## Code Smell
`QuorumResponseHandler.blockFor` had `return (DatabaseDescriptor.getQuorum(table) / 2) + 1` for the QUORUM case. `DatabaseDescriptor.getQuorum` was defined as `(replicationFactor / 2) + 1`, so the actual threshold computed was `((RF/2 + 1) / 2) + 1`, which is less than a true quorum for RF > 3.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: double-applied-formula
- severity: silent-wrong-result
- subsystem: paxos-lwt

## Root Cause
`getQuorum` was a helper that computed the quorum threshold. `blockFor` mistakenly called `getQuorum` and then applied `/ 2 + 1` again to the result. For RF=3, the result happened to be 2 (correct), masking the bug. For RF=5, `getQuorum` returned 3, then `blockFor` returned `(3/2)+1 = 2` instead of the correct 3. The fix removes `getQuorum` and directly computes `(getReplicationFactor(table) / 2) + 1` in `blockFor`.

## Example
```java
// BEFORE — quorum of quorum (wrong for RF > 3)
case QUORUM:
    return (DatabaseDescriptor.getQuorum(table) / 2) + 1;
// where getQuorum = (RF/2)+1, so result = ((RF/2+1)/2)+1
```
```java
// AFTER — correct single application
case QUORUM:
    return (DatabaseDescriptor.getReplicationFactor(table) / 2) + 1;
```
