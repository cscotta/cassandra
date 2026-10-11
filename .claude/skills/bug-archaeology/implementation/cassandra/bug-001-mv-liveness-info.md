# Bug 001: TTLed MV Row Re-appears After Cursor Compaction

**Commit**: 1a9ab95c20 · **Ticket**: CASSANDRA-21152
**Subsystem**: Materialized Views / Cursor-based Compaction
**Category**: Missing override of interface `default` that returns a wrong constant

---

## Root Cause

MV PK tombstones are encoded with sentinel `ttl = EXPIRED_LIVENESS_TTL (Integer.MAX_VALUE)`. During cursor compaction they are deserialized into `ReusableLivenessInfo`, which never overrode `isExpired()`. The merge step pivots on it:

```java
// LivenessInfo.supersedes() — expired liveness must always win
if (isExpired() ^ other.isExpired())
    return isExpired();
```

With `isExpired()` always `false` (inherited default), the XOR never fired — live liveness from another SSTable won, resurrecting the deleted MV row.

### Before / After

```java
// LivenessInfo.java BEFORE — default hides the bug at compile time
default boolean isExpired() { return false; }

// LivenessInfo.java AFTER — promoted to abstract, forces every implementor to choose
boolean isExpired();
```

```java
// ReusableLivenessInfo.java AFTER — checks the actual deserialized sentinel
@Override
public boolean isExpired() { return ttl == EXPIRED_LIVENESS_TTL; }
```

---

## How to Spot

`default` returning a constant is safe only for the immutable base case. Any `Reusable*`/`Mutable*` subclass that deserializes external state with sentinel values needs to override it. Promoting `default` → `abstract` forces the audit at compile time.

---

## Test

`ViewCompactionTest.testCompactionOfDeletedRowWithTtl`: write TTL row → flush → delete non-PK col → flush → `compact(MV)` → assert row absent. Pre-fix: row reappears after the second compact.
