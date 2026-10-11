# Bug 0580: BigQuery client ignores credentials configured in BigQueryOptions

**Commit**: 554a3c1d2ad3faf1397f763c8ae9b1e69c9bb55d · **Ticket**: #14447

## Detection Rule

When a method accepts a configuration object (e.g. `BigQueryOptions`) that already carries credentials, verify that the implementation actually reads credentials from that object rather than re-fetching them independently from a global source.

## Code Smell

A constructor or init method receives a fully-configured options/settings object but then separately calls a global credential provider (e.g. `GoogleCredentials.getApplicationDefault()`) instead of reading `options.getCredentials()`, making the credentials field of the options object silently unreachable.

## Tags

- scope: universal
- category: silent-skip
- code-pattern: passed-config-field-ignored
- severity: silent-wrong-result
- subsystem: gcp/bigquery-catalog

## Root Cause

`BigQueryMetastoreClientImpl` accepted `BigQueryOptions` as its constructor parameter but always called `GoogleCredentials.getApplicationDefault()` to build the HTTP credential adapter instead of reading the credentials already embedded in the options object via `options.getCredentials()`. Any credentials set on `BigQueryOptions` by the caller (e.g. impersonated credentials) were therefore silently discarded, and the client always authenticated as the ambient application-default identity.

## Example

```java
// BEFORE
private void initClient(BigQueryOptions options)
    throws IOException, GeneralSecurityException {
  HttpCredentialsAdapter httpCredentialsAdapter =
      new HttpCredentialsAdapter(
          GoogleCredentials.getApplicationDefault().createScoped(BigqueryScopes.all()));
  this.client = new Bigquery.Builder(...)
      .setHttpRequestInitializer(httpCredentialsAdapter)
      .build();
}
```

```java
// AFTER
private void initClient(BigQueryOptions options)
    throws IOException, GeneralSecurityException {
  GoogleCredentials credentials = (GoogleCredentials) options.getCredentials();
  HttpCredentialsAdapter httpCredentialsAdapter = new HttpCredentialsAdapter(credentials);
  this.client = new Bigquery.Builder(...)
      .setHttpRequestInitializer(httpCredentialsAdapter)
      .build();
}
```
