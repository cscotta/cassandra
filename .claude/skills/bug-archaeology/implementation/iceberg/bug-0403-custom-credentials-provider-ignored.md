# Bug 0403: Custom AWS credential provider silently ignored when refresh endpoint is also set

**Commit**: 48da548979099d0c33c8f39062f728175f786538 · **Ticket**: #15249

## Detection Rule

When a method selects among multiple provider strategies by checking conditions in sequence, verify that a more-specific/explicit configuration (user-supplied class) is checked before a less-specific/default one (auto-refresh), not after it.

## Code Smell

An explicit user-provided `clientCredentialsProvider` check appears after an automatic `refreshCredentialsEnabled` check in the same if-else chain, so a user's custom provider is silently bypassed whenever refresh is also enabled.

## Tags

- scope: project-specific
- category: logic-error
- code-pattern: wrong-priority-condition-order
- severity: silent-wrong-result
- subsystem: aws/credentials

## Root Cause

`AwsClientProperties.credentialsProvider()` evaluated `refreshCredentialsEnabled` before checking whether `clientCredentialsProvider` was set. When both were configured, the vended-credentials refresh path was always taken, and the explicitly named custom provider class was never instantiated. The fix moves the custom-provider check to the top of the method so it takes unconditional precedence.

## Example

```java
// BEFORE
public AwsCredentialsProvider credentialsProvider(...) {
    if (refreshCredentialsEnabled && !Strings.isNullOrEmpty(refreshCredentialsEndpoint)) {
        // vended credentials path — wins even when user set clientCredentialsProvider
        ...
    }
    if (!Strings.isNullOrEmpty(this.clientCredentialsProvider)) {
        return credentialsProvider(this.clientCredentialsProvider);
    }
    return DefaultCredentialsProvider.builder().build();
}
```

```java
// AFTER
public AwsCredentialsProvider credentialsProvider(...) {
    if (!Strings.isNullOrEmpty(this.clientCredentialsProvider)) {
        // explicit custom provider wins
        clientCredentialsProviderProperties.put(VendedCredentialsProvider.URI, refreshCredentialsEndpoint);
        return credentialsProvider(this.clientCredentialsProvider);
    }
    if (refreshCredentialsEnabled && !Strings.isNullOrEmpty(refreshCredentialsEndpoint)) {
        ...
    }
    return DefaultCredentialsProvider.builder().build();
}
```
