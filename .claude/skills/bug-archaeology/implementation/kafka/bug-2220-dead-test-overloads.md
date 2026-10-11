**Commit**: b2380d7bf6 · **Ticket**: KAFKA-17772
**Subsystem**: server/test (ReplicationControlManagerTest)
**Category**: State not cleaned up

**Root cause**
`ReplicationControlManagerTest` contained overloads `unfenceBrokers(Set<Integer>)` and `inControlledShutdownBrokers(Set<Integer>)` that were only ever called from their varargs siblings. The overloads were dead code: they added surface area and could be confused with the canonical varargs versions by future contributors. Removing them forces all callers through a single, uniform interface.

**Before/After**
```java
// Before – redundant overload
void unfenceBrokers(Set<Integer> brokerIds) { ... }
void unfenceBrokers(Integer... brokerIds) {
    unfenceBrokers(new HashSet<>(Arrays.asList(brokerIds)));
}

// After – only varargs version remains
void unfenceBrokers(Integer... brokerIds) { ... }
```

**How to spot**
In test helper classes, check for overloads where one simply wraps the other with a collection conversion. If the wrapper is never called directly from test methods, remove it.

**Test**
No test added.
