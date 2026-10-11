# Bug 0596: Catalog properties not passed to credential-refresh provider properties

**Commit**: 055a73a92cc2f14b8adbd7b0851d6c5b8538db5c · **Ticket**: #14608

## Detection Rule

When a credentials provider is constructed from a prefix-stripped subset of catalog properties, verify that the full set of non-prefixed catalog properties (e.g. `uri`, `token`) is also merged in, so that downstream credential calls have access to properties not expressed under the prefix.

## Code Smell

`PropertyUtil.propertiesWithPrefix(properties, PREFIX)` used alone to build `clientCredentialsProviderProperties`, followed later by a deferred `putAll(allProperties)` call inside a method that may not always execute — meaning the full catalog properties are only conditionally or belatedly available to the provider.

## Tags

- scope: project-specific
- category: silent-skip
- code-pattern: missing-config-propagation
- severity: silent-wrong-result
- subsystem: aws/credentials/AwsClientProperties

## Root Cause

`AwsClientProperties` built `clientCredentialsProviderProperties` using only the `client.credentials-provider.` prefixed subset of catalog properties. The full catalog properties (including `uri`, `token`, etc.) were merged in lazily inside `credentialsProvider()` via `clientCredentialsProviderProperties.putAll(allProperties)`, which only ran when the refresh-credentials path was taken. Prefixed properties could also silently shadow or be shadowed by their un-prefixed counterparts unpredictably. The fix merges all non-prefixed properties as a base and overrides them with the prefixed ones at construction time, removing the deferred `putAll` entirely.

## Example

```java
// BEFORE
this.clientCredentialsProviderProperties =
    PropertyUtil.propertiesWithPrefix(properties, CLIENT_CREDENTIAL_PROVIDER_PREFIX);
// ...later, only on the refresh path:
clientCredentialsProviderProperties.putAll(allProperties);
```

```java
// AFTER
this.clientCredentialsProviderProperties =
    PropertyUtil.mergeProperties(
        PropertyUtil.filterProperties(
            properties,
            Predicate.not(p -> p.startsWith(CLIENT_CREDENTIAL_PROVIDER_PREFIX))),
        PropertyUtil.propertiesWithPrefix(properties, CLIENT_CREDENTIAL_PROVIDER_PREFIX));
```
