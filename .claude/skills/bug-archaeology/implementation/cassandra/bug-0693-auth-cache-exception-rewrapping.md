# Bug 0693: Auth Cache get() Wraps Exceptions in ExecutionException Unnecessarily

**Commit**: d8871bd5b4 · **Ticket**: CASSANDRA-13367
**Subsystem**: src/java/org/apache/cassandra/auth
**Category**: Logic error in condition

---

## Root Cause

`AuthCache.get()` declared `throws ExecutionException`, which forced every caller (`PermissionsCache`, `RolesCache`, `PasswordAuthenticator`, `AuthorizationProxy`) to catch `ExecutionException` and re-wrap its cause in a `RuntimeException`. This double-wrapping obscured the original exception type and added unnecessary boilerplate. The load functions supplied to the cache already threw unchecked exceptions (`AuthenticationException`, `RuntimeException`) directly, so `ExecutionException` would never actually be thrown — yet callers had to handle it defensively. The fix was to remove the checked `throws ExecutionException` declaration from `AuthCache.get()`, relying on Guava's `LoadingCache` propagating unchecked exceptions as-is, which eliminated all the re-wrapping at call sites.

### Before / After

```java
// BEFORE — callers forced to re-wrap
public V get(K k) throws ExecutionException { ... }

// PermissionsCache
try {
    return get(Pair.create(user, resource));
} catch (ExecutionException e) {
    throw new RuntimeException(e);
}
```

```java
// AFTER — clean propagation
public V get(K k) { ... }

// PermissionsCache
return get(Pair.create(user, resource));
```

---

## How to Spot

When a method declares a checked exception that its implementation never actually throws (because the underlying mechanism only throws unchecked exceptions), every caller must write boilerplate catch-and-re-wrap code. Review cache `get()` wrappers to confirm whether the declared checked exception can actually reach the caller; if not, removing the declaration eliminates hidden double-wrapping that can change the exception type visible to upstream handlers.

---

## Test

No test added; the change is a structural refactor with no behavioral change under correct inputs.
