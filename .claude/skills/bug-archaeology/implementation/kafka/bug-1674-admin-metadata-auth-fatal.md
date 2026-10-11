## Commit

- Hash: 2b7c039971
- Tickets: KAFKA-18440
- Subject: KAFKA-18440: Treat AuthorizationException as fatal in AdminMetadataManager

## Subsystem

clients / admin / AdminMetadataManager

## Category

Missing null / bounds check

## Root cause

`AdminMetadataManager.handleResponse()` only treated `AuthenticationException`, `MismatchedEndpointTypeException`, `UnsupportedEndpointTypeException`, and `UnsupportedVersionException` as fatal errors that prevented further metadata refreshes. `AuthorizationException` (e.g. `ClusterAuthorizationException`) was not in the list, so an admin client that lacked cluster-describe permission would retry metadata requests indefinitely, burning CPU and flooding broker logs, rather than surfacing a fatal error to the caller immediately.

## Before

```java
private static boolean isFatalException(Exception e) {
    return e instanceof AuthenticationException
        || e instanceof MismatchedEndpointTypeException
        || e instanceof UnsupportedEndpointTypeException
        || e instanceof UnsupportedVersionException;
}
```

## After

```java
private static boolean isFatalException(Exception e) {
    return e instanceof AuthenticationException
        || e instanceof AuthorizationException      // added
        || e instanceof MismatchedEndpointTypeException
        || e instanceof UnsupportedEndpointTypeException
        || e instanceof UnsupportedVersionException;
}
```

## How to spot

`isFatal` / `isRetriable` classification helpers in client metadata managers that omit `AuthorizationException` — authorization failures are permanent for a given credential and should never trigger a retry loop.

## Test

`AdminMetadataManagerTest` extended with `ClusterAuthorizationException` scenario asserting the manager enters the fatal state after the first failure.
