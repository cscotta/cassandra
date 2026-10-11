# Bug 1355: LWT Requiring All Nodes for CAS Due to Missing Parentheses

**Commit**: 61bd68885e · **Ticket**: CASSANDRA-8640
**Subsystem**: src/java/org/apache/cassandra/service/StorageProxy.java
**Category**: Logic error in condition

---

## Root Cause

The Paxos quorum calculation in `StorageProxy` computed the required number of participants for a CAS operation using a missing pair of parentheses. Java operator precedence evaluated `participants + 1 / 2` as `participants + (1 / 2)`, which is `participants + 0` (integer division), meaning `requiredParticipants` equalled the full participant count rather than a majority. This caused every LWT operation to require that *all* nodes be alive instead of a simple majority quorum, making CAS unnecessarily strict about liveness and throwing `UnavailableException` in cases where a quorum was actually available.

### Before / After

```java
// BEFORE — operator precedence bug: evaluates as participants + 0
int requiredParticipants = participants + 1  / 2; // See CASSANDRA-833
```

```java
// AFTER — parentheses ensure ceiling-division for majority quorum
int requiredParticipants = (participants + 1) / 2; // See CASSANDRA-8346, CASSANDRA-833
```

---

## How to Spot

Integer ceiling-division is a well-known idiom: `(n + 1) / 2` or `(n + d - 1) / d`. Whenever this pattern appears without parentheses enclosing the numerator, Java's left-to-right operator precedence will silently compute the wrong value. Reviewers should be especially suspicious of expressions mixing addition and division in quorum or majority calculations — the result without parentheses always compiles and produces a number, but the semantics are completely wrong.

---

## Test

No test added. The fix is a one-character change restoring intended quorum arithmetic; correctness is validated by existing Paxos/LWT integration tests.
