# Bug 0510: Vended credential refresh drops planId breaking remote scan planning

**Commit**: cbf07cba8c89a49e42baa7073ee15bd914d2e1e0 · **Ticket**: #14767

## Detection Rule

When a credential provider refreshes tokens by calling a remote endpoint AND the original request that initiated the scan carried scoping context (e.g., a plan ID), verify that every subsequent credential-refresh HTTP call re-sends that context as a query parameter rather than passing `null`.

## Code Smell

An HTTP GET call to a credentials endpoint where the query-params argument is hardcoded to `null`, while the surrounding class holds a `planId` (or similar scan-scoping field) that was populated during construction.

## Tags

- scope: distributed-systems
- category: state-not-cleaned
- code-pattern: context-not-propagated-on-refresh
- severity: crash
- subsystem: rest-catalog vended-credentials

## Root Cause

During remote scan planning the server issues short-lived, plan-scoped credentials and expects the `planId` to be echoed back on every refresh so it can re-validate scope. The three credential providers (AWS S3, Azure ADLS, GCP GCS) each stored the `planId` from catalog properties but passed `null` as the query-params map when calling the `/credentials` endpoint. Without the `planId` query parameter the server had no way to associate the refresh with the active plan, causing authorization failures when the initial credentials expired mid-scan.

## Example

```java
// BEFORE (all three providers — S3, ADLS, GCS)
return httpClient()
    .get(
        credentialsEndpoint,
        null,                    // query params always null
        LoadCredentialsResponse.class,
        Map.of(),
        ErrorHandlers.defaultErrorHandler());
```

```java
// AFTER
return httpClient()
    .get(
        credentialsEndpoint,
        null != planId ? Map.of("planId", planId) : null,
        LoadCredentialsResponse.class,
        Map.of(),
        ErrorHandlers.defaultErrorHandler());
```
