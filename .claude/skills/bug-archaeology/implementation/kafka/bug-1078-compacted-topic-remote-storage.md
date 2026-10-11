## Commit

15331bbfc4 — KAFKA-19273: validateNoRemoteStorageForCompactedTopic incorrectly checks cleanup policy

## Ticket

KAFKA-19273

## Subsystem

Storage / log configuration validation

## Category

Logic error in condition

## Root cause

`LogConfig.validateNoRemoteStorageForCompactedTopic` checked whether the `cleanup.policy` string contained the substring `"compact"`. This matched `"delete,compact"` as well as `"compact"`, but more critically the cast of the config value to `List<String>` was applied incorrectly — the raw config value is a comma-separated `String`, not a `List`, so the cast threw `ClassCastException` at runtime. Even if the cast had succeeded, the `contains("compact")` check on a list would look for an exact element match, not a substring match, so `"delete,compact"` would silently pass validation and allow a compacted topic to be configured with remote storage, violating the constraint.

## Before / After

```java
// Before — wrong cast, substring check on wrong type
List<String> policies = (List<String>) config.get(CLEANUP_POLICY_CONFIG);
if (policies.contains("compact")) { throw ...; }

// After — correct split, set-based equality check
Set<String> policies = Arrays.stream(
    config.getString(CLEANUP_POLICY_CONFIG).split(","))
    .collect(Collectors.toSet());
if (!policies.equals(Set.of("delete"))) { throw ...; }
```

## How to spot

Search for `(List<String>) config.get(...)` in configuration validators. `AbstractConfig.get()` returns the raw parsed type, which for multi-value string configs is a `List<String>` only if the config is declared as a `LIST` type. If it is declared as a plain `STRING`, the cast throws at runtime. Always use the typed accessor (`getString`, `getList`) to avoid silent class-cast errors.

## Test

`LogConfigTest` was extended with cases for `"compact"`, `"delete,compact"`, and `"delete"` to verify the correct exception behaviour and the no-exception path.
