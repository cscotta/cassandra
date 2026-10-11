# Bug 2224: InetAddress Formatted as Object Reference Instead of Host Address in CQL Query

**Commit**: b7716c76b3 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/SystemTable.java
**Category**: Logic error in condition

---

## Root Cause

When removing tokens from the system peers table, the CQL UPDATE statement was interpolated with `entry.getKey()` — an `InetAddress` object — rather than `entry.getKey().getHostAddress()`. The `InetAddress.toString()` default representation includes a hostname prefix (e.g., `/127.0.0.1`), making the resulting WHERE clause syntactically invalid and the row would not be found or updated correctly.

### Before / After

```java
// BEFORE
processInternal(String.format(req, PEERS_CF, serializeTokens(toRemove), entry.getKey()));
```

```java
// AFTER
processInternal(String.format(req, PEERS_CF, serializeTokens(toRemove), entry.getKey().getHostAddress()));
```

---

## How to Spot

When embedding network address values into string-formatted CQL or SQL queries, always call `.getHostAddress()` on `InetAddress` objects rather than relying on implicit `toString()`. Review any `String.format` calls that include `InetAddress` values for WHERE clauses.

---

## Test

No test added (the commit title references a test failure it fixed, but the fix is in production source).
