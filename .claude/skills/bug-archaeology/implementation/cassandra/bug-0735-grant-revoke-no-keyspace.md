# Bug 0735: GRANT/REVOKE on table fails when keyspace not specified

**Commit**: e4be2d06b7 · **Ticket**: CASSANDRA-13053
**Subsystem**: CQL / Authorization
**Category**: Logic error in condition

---

## Root Cause
`PermissionsManagementStatement` (the base for `GRANT` and `REVOKE`) calls `maybeCorrectResource()` to resolve an unqualified table name (e.g., `GRANT SELECT ON TABLE foo`) to the current keyspace. This correction was only applied inside `execute()`, not inside `checkAccess()`. Because `checkAccess()` runs before `execute()`, it verified permissions against the uncorrected resource (the bare table name without keyspace). When the resolved resource differed from the uncorrected one, `state.ensureHasPermission()` checked the wrong resource, meaning the permission check could pass for a resource the user actually didn't have access to, or fail for a resource they did have access to, depending on the permission table state.

### Before / After
```java
// BEFORE — checkAccess() did not correct the resource
public void checkAccess(ClientState state) throws UnauthorizedException
{
    state.ensureHasPermission(Permission.AUTHORIZE, resource);  // resource may be uncorrected
    for (Permission p : permissions)
        state.ensureHasPermission(p, resource);
}

// AFTER — resource corrected at the top of checkAccess()
public void checkAccess(ClientState state) throws UnauthorizedException
{
    resource = maybeCorrectResource(resource, state);  // resolve keyspace first
    state.ensureHasPermission(Permission.AUTHORIZE, resource);
    for (Permission p : permissions)
        state.ensureHasPermission(p, resource);
}
```

---

## How to Spot
Whenever a statement-level method like `checkAccess()` uses a resource object that is also transformed in `execute()`, verify that the transformation is applied consistently in both places (or factored into a shared pre-processing step). Partial application of name resolution — where one code path resolves and another does not — leads to permission checks against a different resource than the one actually operated on.

---

## Test
No new test added in this commit.
