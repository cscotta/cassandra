# Bug 014: Schema Diffs Miss Changes to Comments and Security Labels

**Commit**: dbfcd52fd52dd2053844244b53e563d9d7a67065 · **Ticket**: CASSANDRA-21045
**Subsystem**: Schema / CQL metadata
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

The `equals()` and `hashCode()` implementations on several schema metadata classes (`ColumnMetadata`, `UserType`, `KeyspaceParams`) did not include the `comment` and `securityLabel` fields. As a result, two schema objects that differed only in a comment or security label were considered equal, so schema diff computations silently discarded such changes — clients received no `SCHEMA_CHANGE` notification and the altered state was never propagated. The root of the structural confusion was that `comment` and `securityLabel` were stored on the base class `ColumnSpecification` (shared with non-metadata query column specs), but the fields were not semantically relevant there; the fix moves them down to `ColumnMetadata` and adds them to all affected equality checks.

### Before / After

```java
// BEFORE — ColumnMetadata.equals() omits comment and securityLabel
return name.equals(other.name)
    && ksName.equals(other.ksName)
    && cfName.equals(other.cfName)
    && Objects.equals(mask, other.mask)
    && Objects.equals(columnConstraints, other.columnConstraints);

// AFTER — comment and securityLabel included
return name.equals(other.name)
    && ksName.equals(other.ksName)
    && cfName.equals(other.cfName)
    && Objects.equals(mask, other.mask)
    && Objects.equals(columnConstraints, other.columnConstraints)
    && comment.equals(other.comment)
    && securityLabel.equals(other.securityLabel);

// BEFORE — KeyspaceParams.equals() omits comment and securityLabel
return durableWrites == p.durableWrites && replication.equals(p.replication) && fastPath.equals(p.fastPath);

// AFTER
return durableWrites == p.durableWrites && replication.equals(p.replication)
    && fastPath.equals(p.fastPath)
    && comment.equals(p.comment) && securityLabel.equals(p.securityLabel);
```

---

## How to Spot

When a class has fields added over time (especially metadata annotation fields like comments or labels), `equals()` and `hashCode()` are a common omission because they are often hand-written and not re-audited when new fields are added. Any code path that detects schema drift by comparing object equality will silently ignore fields left out of `equals`. Reviewers should verify that every field that can be independently mutated by a DDL statement participates in both `equals` and `hashCode`, and that newly added fields are included consistently across the entire inheritance hierarchy.

---

## Test

`SchemaChangesTest` adds eight focused unit tests (e.g., `testColumnComment`, `testKeyspaceSecurityLabel`, `testUserTypeFieldComment`) that build two schema objects differing only in the targeted annotation, assert they are not equal, and verify that the diff API reports exactly one altered entry.
