# Bug 0434: Credential-refresh HTTP clients ignore user-configured headers

**Commit**: b2f312f4396d1b5674e2378dbef3887fb2d789b6 · **Ticket**: #15110

## Detection Rule

When an HTTPClient is constructed for a credential-refresh or token-vending sub-client (not the main catalog client), verify that `withHeaders(RESTUtil.configHeaders(properties))` is called on the builder so that operator-configured request headers (e.g. authorization proxies, tracing headers) are forwarded.

## Code Smell

`HTTPClient.builder(properties).uri(catalogEndpoint).build()` without a `.withHeaders(...)` call inside a `synchronized` lazy-init block for credential-refresh clients, while the main catalog HTTPClient does include the headers.

## Tags

- scope: project-specific
- category: silent-skip
- code-pattern: missing-config-propagation
- severity: silent-wrong-result
- subsystem: aws/s3/vended-credentials, azure/adls, gcp/gcs

## Root Cause

`VendedCredentialsProvider` (AWS), `VendedAdlsCredentialProvider` (Azure), `OAuth2RefreshCredentialsHandler` (GCP), and `S3V4RestSignerClient` each construct a secondary HTTPClient for credential refresh calls. All four builders omitted `withHeaders(RESTUtil.configHeaders(properties))`, so any headers set by the user via catalog properties (e.g. `header.X-Custom-Auth`) were applied to the main catalog client but silently dropped from credential-refresh requests, causing those requests to fail when a header was required by the remote endpoint.

## Example

```java
// BEFORE
HTTPClient httpClient = HTTPClient.builder(properties)
    .uri(catalogEndpoint)
    .build();
```

```java
// AFTER
HTTPClient httpClient = HTTPClient.builder(properties)
    .uri(catalogEndpoint)
    .withHeaders(RESTUtil.configHeaders(properties))
    .build();
```
