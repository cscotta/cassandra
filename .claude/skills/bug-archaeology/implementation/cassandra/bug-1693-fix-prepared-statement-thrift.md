# Bug 1693: Fix Prepared Statement on Thrift Post-6659

**Commit**: 79da4a6e0e · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/cql3/QueryProcessor.java
**Category**: Wrong constant / default value

---

## Root Cause

CASSANDRA-6659 introduced the `QueryHandler` interface and moved `prepare()` to use `QueryState`, but the implementation always passed `forThrift = false` to the underlying `prepare(String, ClientState, boolean)` overload, regardless of whether the caller was a Thrift client. Prepared statements prepared via the Thrift interface were therefore not cached under the Thrift-specific key, causing them to be unresolvable at execution time. The fix checks whether the `ClientState` is an instance of `ThriftClientState` and sets `forThrift` accordingly.

### Before / After

```java
// BEFORE – forThrift always false, Thrift prepared statements not cached correctly
public ResultMessage.Prepared prepare(String queryString, QueryState queryState) {
    return prepare(queryString, queryState.getClientState(), false);
}
```

```java
// AFTER – forThrift derived from actual client type
public ResultMessage.Prepared prepare(String queryString, QueryState queryState) {
    ClientState cState = queryState.getClientState();
    return prepare(queryString, cState, cState instanceof ThriftClientState);
}
```

---

## How to Spot

After refactoring a method to accept a flag that distinguishes two code paths, verify that every call site that invokes the new signature derives the flag from context rather than hardcoding a constant. A hardcoded `false` (or `true`) is a strong signal that the flag may be wrong for some callers.

---

## Test

No test added.
