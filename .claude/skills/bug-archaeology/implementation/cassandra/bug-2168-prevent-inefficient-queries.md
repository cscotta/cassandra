# Bug 2168: Missing Guard Against Potentially Inefficient Filtering Queries in CQL3

**Commit**: 6fb7512400 · **Ticket**: CASSANDRA-4915
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java, src/java/org/apache/cassandra/cql3/Cql.g
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

CQL3 SELECT statements that required server-side data filtering — such as range queries with non-indexed column predicates or queries with more than one index expression — were executed without warning, potentially scanning large amounts of data with unpredictable latency. There was no mechanism to distinguish intentional full-scan queries from accidental ones. The fix introduced an `ALLOW FILTERING` clause that must be explicitly provided when a query might involve filtering, making the performance risk visible at the query language level.

### Before / After

```java
// BEFORE — no check; inefficient filtering queries ran silently
return new ParsedStatement.Prepared(stmt, Arrays.<ColumnSpecification>asList(names));
```

```java
// AFTER — reject key-range queries that would filter unless ALLOW FILTERING is set
if (!parameters.allowFiltering && stmt.isKeyRange)
{
    if (stmt.metadataRestrictions.size() > 1 || (stmt.metadataRestrictions.isEmpty() && !stmt.columnFilterIsIdentity()))
        throw new InvalidRequestException("Cannot execute this query as it might involve data filtering...");
}
return new ParsedStatement.Prepared(stmt, Arrays.<ColumnSpecification>asList(names));
```

---

## How to Spot

During review, look for range-scan code paths that apply additional column-level predicates after the initial data read without any guard. Any query that conditionally filters rows after fetching them should require an explicit opt-in from the caller.

---

## Test

No test added.
