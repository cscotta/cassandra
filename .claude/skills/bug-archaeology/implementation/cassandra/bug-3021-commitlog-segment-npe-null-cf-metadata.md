# Bug 3021: NPE in CommitLogSegment debug log when CF metadata is null

**Commit**: fb05f27d51 · **Ticket**: CASSANDRA-3021

## Detection Rule
When a ternary expression is written as `m == null ? m.cfName : "<deleted>"`, swap the branches so the null case does not dereference the null reference.

## Code Smell
`sb.append(m == null ? m.cfName : "<deleted>")` dereferences `m` in the true branch of a null check, causing a NullPointerException at the one moment the check is relevant.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: ternary-null-dereference-inverted-branches
- severity: crash
- subsystem: commitlog-cdc

## Root Cause
`CommitLogSegment.dirtyString()` built a diagnostic string by checking `m == null` and then accessing `m.cfName` in the null branch — the ternary branches were swapped. When debug logging was enabled and a dropped CF appeared in the commit log segment, the null-check always caused an NPE rather than printing `"<deleted>"`.

## Example
```java
// BEFORE (branches are swapped)
sb.append(m == null ? m.cfName : "<deleted>").append(" (").append(cfId).append("), ");
```
```java
// AFTER
sb.append(m == null ? "<deleted>" : m.cfName).append(" (").append(cfId).append("), ");
```
