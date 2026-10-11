# Bug 0432: Retry policy not applied to Glue and DynamoDB clients

**Commit**: 738c7a4753552ff990401a5f736fa29ad26d5c60 · **Ticket**: #15094

## Detection Rule

When a builder pattern applies multiple configuration mutations (credentials, HTTP client settings, endpoints) to an AWS SDK client, verify that the retry configuration mutation is included alongside the others on every client builder.

## Code Smell

AWS client builders that call `applyMutation(httpClientProperties::applyHttpClientConfigurations)` and credential/endpoint mutations but omit `applyMutation(awsClientProperties::applyRetryConfigurations)`, while sibling clients (e.g. S3) do include it.

## Tags

- scope: project-specific
- category: silent-skip
- code-pattern: missing-config-mutation
- severity: performance
- subsystem: aws/client-factory

## Root Cause

`AwsClientFactories` and `AssumeRoleAwsClientFactory` both build Glue and DynamoDB clients using a chain of `applyMutation` calls, but omitted `awsClientProperties::applyRetryConfigurations` from both builders while it was applied to other clients such as S3. As a result, user-configured retry settings (e.g. adaptive retry mode, max attempts) were silently ignored for Glue and DynamoDB operations.

## Example

```java
// BEFORE
return GlueClient.builder()
    .applyMutation(this::applyAssumeRoleConfigurations)
    .applyMutation(httpClientProperties::applyHttpClientConfigurations)
    .build();
```

```java
// AFTER
return GlueClient.builder()
    .applyMutation(this::applyAssumeRoleConfigurations)
    .applyMutation(httpClientProperties::applyHttpClientConfigurations)
    .applyMutation(awsClientProperties::applyRetryConfigurations)
    .build();
```
