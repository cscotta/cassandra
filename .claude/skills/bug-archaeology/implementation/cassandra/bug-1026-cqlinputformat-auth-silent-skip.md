# Bug 1026: CqlInputFormat Ignores Username/Password When No Custom AuthProvider Set

**Commit**: fee9791b0a · **Ticket**: CASSANDRA-10717
**Subsystem**: Hadoop / CQL input format
**Category**: Silent skip on non-match

---

## Root Cause
`CqlConfigHelper.getAuthProvider()` checked whether a custom `AuthProvider` class name was configured. If none was set it immediately returned `Optional.absent()`, silently discarding any plain-text `username`/`password` credentials the caller had configured. This was a backward-incompatible regression: before the custom-provider feature was added, specifying a username and password was the only way to authenticate, so callers that relied on the plain-text credentials path without configuring an explicit provider class suddenly received no authentication at all.

### Before / After
```java
// BEFORE — username/password silently ignored when no custom provider class is set
private static Optional<AuthProvider> getAuthProvider(Configuration conf) {
    Optional<String> authProvider = getInputNativeAuthProvider(conf);
    if (!authProvider.isPresent())
        return Optional.absent();   // <-- credentials silently dropped
    return Optional.of(getClientAuthProvider(authProvider.get(), conf));
}

// AFTER — fall back to plain-text provider when no custom class is specified
private static Optional<AuthProvider> getDefaultAuthProvider(Configuration conf) {
    Optional<String> username = getStringSetting(USERNAME, conf);
    Optional<String> password = getStringSetting(PASSWORD, conf);
    if (username.isPresent() && password.isPresent())
        return Optional.of(new PlainTextAuthProvider(username.get(), password.get()));
    else
        return Optional.absent();
}

private static Optional<AuthProvider> getAuthProvider(Configuration conf) {
    Optional<String> authProvider = getInputNativeAuthProvider(conf);
    if (!authProvider.isPresent())
        return getDefaultAuthProvider(conf);   // <-- use credentials if present
    return Optional.of(getClientAuthProvider(authProvider.get(), conf));
}
```

---

## How to Spot
When extending a configuration method with a new "explicit" path, verify that the old implicit path (e.g. plain credentials) is still reachable as the default. The pattern `if (!x.isPresent()) return absent()` is a red flag when the callers may rely on an older, simpler credential mechanism — check whether the fallback should instead delegate to a legacy helper.

---

## Test
No test added.
