# Bug 0217: JDBC ResultSet and PreparedStatement objects not closed on exception paths in JdbcCatalog

**Commit**: bb37293484468f0502de9986955860505aef9776 · **Ticket**: #15462

## Detection Rule

When a JDBC ResultSet or PreparedStatement is opened inside a try block, verify it is declared in a try-with-resources statement so it is guaranteed to be closed on both normal and exceptional exit paths.

## Code Smell

`ResultSet rs = sql.executeQuery()` (or `conn.prepareStatement(...).execute()`) followed by manual use of `rs` outside a try-with-resources block; if an exception occurs between open and `rs.close()`, the resource is silently leaked.

## Tags

- scope: java
- category: resource-leak
- code-pattern: close-not-on-all-paths
- severity: performance
- subsystem: core/jdbc/JdbcCatalog,JdbcUtil

## Root Cause

`JdbcUtil.tableOrView()`, `JdbcCatalog.atomicCreateTable()`, and `JdbcCatalog.updateSchemaIfRequired()` all opened `ResultSet` or `PreparedStatement` objects without try-with-resources. Any exception thrown between the open call and the explicit close (or in the case of `PreparedStatement`, the statement was never assigned to a variable at all) left the JDBC resource unclosed, leaking database connections over time.

## Example

```java
// BEFORE (JdbcUtil.tableOrView)
ResultSet rs = sql.executeQuery();
if (rs.next()) {
    tableOrView.put(..., rs.getString(...));
}
rs.close();  // skipped if rs.next() throws
```

```java
// AFTER
try (ResultSet rs = sql.executeQuery()) {
    if (rs.next()) {
        tableOrView.put(..., rs.getString(...));
    }
}
```
