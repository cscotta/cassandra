# Bug 0302: QueryProcessor Internal Queries Use Default nowInSec Instead of Caller-Supplied Value

**Commit**: 9b4784bdb7 · **Ticket**: CASSANDRA-17458
**Subsystem**: src/java/org/apache/cassandra/cql3/QueryProcessor.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

Several internal query paths in `QueryProcessor` computed `nowInSec` (used for TTL expiry evaluation and tombstone purging) but then passed it only to `ReadQuery`/`SelectStatement`, not to the `QueryOptions` object. The `QueryOptions` was built with `makeInternalOptions()`, which did not inject `nowInSec`, so the statement re-derived its own time value independently. This created a subtle inconsistency where expiry decisions could differ between the options and the read command built from them, potentially returning rows that should have been filtered as expired (or vice versa). The fix introduces `makeInternalOptionsWithNowInSec()` and threads the same `nowInSec` value through both the options and the query execution path across all affected call sites.

### Before / After

```java
// BEFORE
QueryOptions options = makeInternalOptions(prepared.statement, values);
// ... nowInSec computed separately:
int nowInSec = FBUtilities.nowInSeconds();
ReadQuery readQuery = select.getQuery(options, nowInSec);
```

```java
// AFTER
int nowInSec = FBUtilities.nowInSeconds();
QueryOptions options = makeInternalOptionsWithNowInSec(prepared.statement, nowInSec, values);
ReadQuery readQuery = select.getQuery(options, nowInSec);
```

---

## How to Spot

Whenever a time-sensitive value like `nowInSec` is computed for use in query execution, it must be captured once and propagated consistently to every component that consults it—both the `QueryOptions` and the resulting `ReadQuery`/`SelectStatement`. A common mistake is computing the value late or in multiple places, allowing clock drift between the options object and the execution path and producing inconsistent TTL/expiry behavior.

---

## Test

No new test added in this commit.
