# Bug 2922: TTL value of 0 or negative treated as "no TTL" instead of rejected

**Commit**: a5fa451c2b · **Ticket**: CASSANDRA-1003

## Detection Rule
When a numeric field that should only accept positive values is silently clamped with `Math.max(value, 0)` instead of validated, clients can submit zero or negative TTLs that produce incorrect expiry behavior.

## Code Smell
`Math.max(column.ttl, 0)` silently converts a negative or zero TTL to 0, which is then used as "no TTL"; the Thrift-generated `isSetTtl()` API exists precisely to distinguish "not provided" from "provided as 0".

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: silent-clamp-instead-of-validate
- severity: silent-wrong-result
- subsystem: cql-query

## Root Cause
The Thrift `Column` struct uses `isSetTtl()` to differentiate between a TTL that was explicitly provided and one that was omitted. The server was using `Math.max(column.ttl, 0)` to store the TTL, which meant a client sending `ttl=0` or `ttl=-1` would silently create a column with no TTL rather than receiving an error. The fix validates the TTL via `ThriftValidation.validateTtl()` and uses `column.ttl` directly, rejecting non-positive TTL values with an `InvalidRequestException`.

## Example
```java
// BEFORE
rm.add(..., column.value, column.timestamp,
    Math.max(column.ttl, 0));  // ttl=0 or -1 silently means "no expiry"
```
```java
// AFTER
// validation added:
if (column.isSetTtl() && column.ttl <= 0)
    throw new InvalidRequestException("ttl must be positive");
// then direct use:
rm.add(..., column.value, column.timestamp, column.ttl);
```
