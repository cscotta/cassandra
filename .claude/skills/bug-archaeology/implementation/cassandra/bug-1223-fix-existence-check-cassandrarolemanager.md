# Bug 1223: Wrong Variable in NULL_ROLE Existence Check in CassandraRoleManager

**Commit**: 76cbddebc7 · **Ticket**: CASSANDRA-9551
**Subsystem**: src/java/org/apache/cassandra/auth/CassandraRoleManager.java
**Category**: Logic error in condition

---

## Root Cause

`CassandraRoleManager.collectRoles()` traverses the role-membership graph recursively. For each member role name it calls `getRole(memberOf)` and stores the result in a local variable `granted`, then checks whether the looked-up role is the sentinel `NULL_ROLE`. The check mistakenly compared the outer loop variable `role` against `NULL_ROLE` instead of the freshly fetched `granted`. When a member role does not exist in the store, `getRole` returns `NULL_ROLE` into `granted`, but the guard `if (role.equals(NULL_ROLE))` always tests the parent role — which is never `NULL_ROLE` at that point. The non-existent child role is therefore never skipped, causing a subsequent `granted.name` access on the sentinel to produce wrong results or a spurious entry in the collected set.

### Before / After

```java
// BEFORE
Role granted = getRole(memberOf);
if (role.equals(NULL_ROLE))   // tests the parent, not the child
    continue;
collected.add(RoleResource.role(granted.name));
```

```java
// AFTER
Role granted = getRole(memberOf);
if (granted.equals(NULL_ROLE))  // correctly tests the child lookup
    continue;
collected.add(RoleResource.role(granted.name));
```

---

## How to Spot

When a function fetches a value into a new variable and then immediately guards against a sentinel, confirm the guard names the new variable, not a previously-bound outer variable. The pattern `X = lookup(y); if (outerVar.equals(SENTINEL))` is almost always a copy-paste mistake. Code review should flag any equality check immediately after a variable assignment where the checked variable does not match the just-assigned one.

---

## Test

No test added.
