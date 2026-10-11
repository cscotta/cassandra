# Bug 2861: Read repair hasData check inverted: returns true for digest responses

**Commit**: f47145d3c1 · **Ticket**: CASSANDRA-1622

## Detection Rule
When checking whether any collected read response carries actual row data (as opposed to a digest), the condition must be `!result.isDigestQuery()` — a response with data is a non-digest response.

## Code Smell
`if (result.isDigestQuery()) return true` returns `true` when it finds a *digest* response, meaning `hasData()` claims data is present based on digest-only replies. Read repair then either skips fetching full data or triggers a spurious repair when only digest responses are available.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: inverted-boolean-predicate
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
A merge regression inverted the predicate in `ReadResponseResolver.hasData()`. The method is supposed to return `true` when at least one response contains full row data. `isDigestQuery()` returns `true` for digest-only responses (not data responses), so the condition needed to be negated. With the bug, quorum reads could return stale data after a digest mismatch because the repair fetch was not triggered correctly.

## Example
```java
// BEFORE
if (result.isDigestQuery())
    return true;  // wrong: this is a digest, not data

// AFTER
if (!result.isDigestQuery())
    return true;  // correct: this response carries actual row data
```
