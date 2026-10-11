# Bug 0119: Aliyun module leaks transitive runtime dependencies into consumer classpath

**Commit**: e8b619148b557c5d72522e7f24bdadc9170eabfe · **Ticket**: #15858

## Detection Rule

When a Gradle module that is intended as an optional integration module declares a third-party SDK dependency as `implementation` rather than `compileOnly`, verify that the dependency should actually appear on downstream consumers' runtime classpaths and is not already provided by the runtime environment.

## Code Smell

A `build.gradle` for an optional integration module (e.g., a cloud-provider connector) where third-party SDK artifacts appear under `implementation` alongside other clearly optional peers that are already declared `compileOnly`.

## Tags

- scope: project-specific
- category: resource-leak
- code-pattern: implementation-instead-of-compileonly
- severity: silent-wrong-result
- subsystem: aliyun/build configuration

## Root Cause

`aliyun.credentials.java` and `aliyun.tea` were declared as `implementation` dependencies in the `iceberg-aliyun` module instead of `compileOnly`, causing them to be resolved into the runtime classpath of every project that depended on `iceberg-aliyun`. This silently inflated transitive dependency sets and could introduce version conflicts or unlicensed artifacts into downstream distributions.

## Example

```groovy
// BEFORE — leaked into consumers' runtime classpath
compileOnly libs.aliyun.sdk.oss
implementation libs.aliyun.credentials.java  // leaked
implementation libs.aliyun.tea               // leaked
compileOnly libs.jaxb.api
```

```groovy
// AFTER — consistent: all Aliyun SDK libs are compileOnly
compileOnly libs.aliyun.sdk.oss
compileOnly libs.aliyun.credentials.java
compileOnly libs.aliyun.tea
compileOnly libs.jaxb.api
```
