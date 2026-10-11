# Bug 1110: Avoid Returning Permission.NONE When Failing to Query Users Table

**Commit**: 978660d998 · **Ticket**: CASSANDRA-10168
**Subsystem**: src/java/org/apache/cassandra/auth/CassandraAuthorizer.java
**Category**: Wrong constant / default value

---

## Root Cause

`CassandraAuthorizer.authorize` caught `RequestExecutionException` — which covers transient failures such as timeouts, coordinator errors, and unavailable exceptions — and silently returned `Permission.NONE` (no permissions). This meant that any transient failure querying `system_auth.permissions` would cause the authorizer to deny all permissions, effectively locking out authenticated users whenever the auth table was temporarily unreachable. The correct behaviour for an execution failure is to propagate the exception so the caller can distinguish "permission denied" from "authorization subsystem unavailable."

### Before / After

```java
// BEFORE
catch (RequestExecutionException e)
{
    logger.warn("CassandraAuthorizer failed to authorize {} for {}", user, resource);
    return Permission.NONE;
}
```

```java
// AFTER
catch (RequestExecutionException e)
{
    logger.warn("CassandraAuthorizer failed to authorize {} for {}", user, resource);
    throw new RuntimeException(e);
}
```

---

## How to Spot

In security-sensitive code, a `catch` block that returns a "deny all" sentinel value on infrastructure failure conflates two distinct failure modes: explicit denial and system unavailability. Review every catch in an authorizer or authenticator that returns a permission constant or empty set — if the exception represents a transient I/O or availability problem rather than a deliberate denial, it should be rethrown (or wrapped) so the caller can apply appropriate retry or fail-open/fail-closed policy.

---

## Test

No test added.
