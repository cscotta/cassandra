# Bug 0922: SelectStatement Public API Exposes Internal getLimit(Term, QueryOptions)

**Commit**: d8036f9361 · **Ticket**: CASSANDRA-7017
**Subsystem**: CQL / SelectStatement
**Category**: Logic error in condition

---

## Root Cause
`SelectStatement.getLimit(Term, QueryOptions)` was `public`, exposing an implementation detail of how limits are evaluated. Custom `QueryHandler` implementations were expected to call this method, but the two-argument signature leaked the internal `Term` field; they had to pass back the same field that `SelectStatement` already held, creating tight coupling. There was also no public accessor for `perPartitionLimit`. The fix made the two-argument overload `private` and introduced clean single-argument public methods `getLimit(QueryOptions)` and `getPerPartitionLimit(QueryOptions)` for external callers.

### Before / After
```java
// BEFORE — one public method receiving the internal Term field
public int getLimit(Term limit, QueryOptions options) { ... }

// Called from within the class:
int userLimit = getLimit(limit, options);
int userPerPartitionLimit = getLimit(perPartitionLimit, options);

// AFTER — private implementation, two clean public facades
public int getLimit(QueryOptions options) {
    return getLimit(limit, options);
}
public int getPerPartitionLimit(QueryOptions options) {
    return getLimit(perPartitionLimit, options);
}
private int getLimit(Term limit, QueryOptions options) { ... }
```

---

## How to Spot
When a method that accesses `private` fields is made `public` purely as a convenience for subclasses or custom handlers, check whether the signature forces callers to re-supply data the class already owns. If callers always pass the same private field back, the method is leaking implementation details. Introduce a zero-extra-argument facade and make the raw version private.

---

## Test
No test added.
