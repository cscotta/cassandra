# Bug 0113: SimpleClient Never Releases Acquired Backpressure Capacity

**Commit**: 291cbcccf6 · **Ticket**: CASSANDRA-20202
**Subsystem**: `src/java/org/apache/cassandra/transport`
**Category**: Resource leak

---

## Root Cause

`SimpleClient` overrode the single-argument `processRequest(Envelope request)` method to call `releaseCapacity(...)` after delegating to `super`. However, `CQLMessageHandler` removed that single-argument override entirely in favour of the two-argument `processRequest(Envelope request, Overload backpressure)` form. Because Java method resolution found no matching single-argument method in the superclass chain, `SimpleClient`'s override became a dead, never-called method, so `releaseCapacity` was never invoked and acquired backpressure capacity leaked for every request.

### Before / After

```java
// BEFORE – overrides a method that no longer exists in the superclass
protected boolean processRequest(Envelope request) {
    boolean continueProcessing = super.processRequest(request);
    releaseCapacity(Ints.checkedCast(request.header.bodySizeInBytes));
    return continueProcessing;
}
```

```java
// AFTER – overrides the correct two-argument form that is actually called
protected boolean processRequest(Envelope request, Overload overload) {
    boolean continueProcessing = super.processRequest(request, overload);
    releaseCapacity(Ints.checkedCast(request.header.bodySizeInBytes));
    return continueProcessing;
}
```

---

## How to Spot

When a superclass changes the signature of a method that subclasses rely on for resource cleanup (acquire/release, open/close), the old override silently becomes a new, unreachable method rather than a compile error. The `@Override` annotation would have caught this immediately; its absence on the old method was the missing safeguard. During review, verify that every resource-release path is annotated with `@Override` so that signature drift is a compile-time failure rather than a silent resource leak.

---

## Test

No dedicated unit test added; the fix was validated by manual inspection and the existing transport integration tests.
