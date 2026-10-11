# Bug 0299: JDBC catalog does not recognize Postgres duplicate-key SQLState

**Commit**: b7a519a149961672274c3954afeddb717477d853 · **Ticket**: #15434

## Detection Rule

When catching `SQLException` to detect constraint violations across multiple database backends, verify that the detection covers all vendor-specific signaling paths (exception subclass, SQLState code, and message text) rather than relying on only one or two.

## Code Smell

A `catch (SQLException e)` block checks only `instanceof SQLIntegrityConstraintViolationException` and/or a message substring like `"constraint failed"`, with no check of `e.getSQLState()`; a comment mentions SQLite as the only special case, omitting other databases such as PostgreSQL that signal via SQLState.

## Tags

- scope: project-specific
- category: wrong-filter-result
- code-pattern: incomplete-exception-predicate
- severity: crash
- subsystem: jdbc-catalog

## Root Cause

PostgreSQL's JDBC driver reports unique-constraint violations by setting SQLState `"23505"` on a plain `SQLException` rather than throwing `SQLIntegrityConstraintViolationException`. The existing code only matched on the subclass or the SQLite message text `"constraint failed"`, so Postgres duplicate-key errors fell through to `throw new UncheckedSQLException(e, "Unknown failure")` instead of the expected `AlreadyExistsException`. The fix centralizes constraint-violation detection in `JdbcUtil.isConstraintViolation()` which checks all three paths.

## Example

```java
// BEFORE: Postgres SQLState "23505" falls through to unknown failure
} catch (SQLIntegrityConstraintViolationException e) {
    throw new AlreadyExistsException(e, "Table already exists: %s", tableIdentifier);
} catch (SQLException e) {
    // SQLite doesn't set SQLState or throw SQLIntegrityConstraintViolationException
    if (e.getMessage() != null && e.getMessage().contains("constraint failed")) {
        throw new AlreadyExistsException("Table already exists: %s", tableIdentifier);
    }
    throw new UncheckedSQLException(e, "Unknown failure");
}
```

```java
// AFTER: unified helper covers all three vendor signals
static boolean isConstraintViolation(SQLException ex) {
    return ex instanceof SQLIntegrityConstraintViolationException
        || POSTGRES_UNIQUE_VIOLATION_SQLSTATE.equals(ex.getSQLState())
        || (ex.getMessage() != null && ex.getMessage().contains("constraint failed"));
}
// callers simply: if (JdbcUtil.isConstraintViolation(e)) { throw AlreadyExistsException ... }
```
