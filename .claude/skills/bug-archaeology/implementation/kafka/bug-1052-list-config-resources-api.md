## Commit

30d7c71f09 — KAFKA-18904 [2/N]: Add listConfigResources to Admin interface (KIP-1142)

## Ticket

KAFKA-18904

## Subsystem

Admin client / configuration resources API

## Category

Wrong constant / default value

## Root cause

`Admin.listClientMetricsResources()` was the only way to list named configuration resources, but it was scoped exclusively to client-metrics resources. KIP-1142 generalised this into `listConfigResources(Set<ConfigResource.Type>, ListConfigResourcesOptions)`, which supports any `ConfigResource.Type`. The old method was silently limited to one resource type, making it impossible for callers to discover other named resource types (e.g., broker loggers) through the same API surface. The fix deprecates the old method and introduces the new generalised overload in `Admin`, `KafkaAdminClient`, and `ForwardingAdmin`.

## Before / After

```java
// Before — type-locked method
ListConfigResourcesResult listClientMetricsResources();

// After — generalised method accepting any resource type set
ListConfigResourcesResult listConfigResources(
    Set<ConfigResource.Type> configResourceTypes,
    ListConfigResourcesOptions options);

@Deprecated
default ListConfigResourcesResult listClientMetricsResources() {
    return listConfigResources(Set.of(ConfigResource.Type.CLIENT_METRICS), ...);
}
```

## How to spot

When a method name encodes a specific resource type that is actually stored in a generic container, the method will become a silent dead-end as new types are added. Search for Admin API methods whose names are scoped to a single concept but whose underlying protocol supports a type parameter.

## Test

`KafkaAdminClientTest` and `ForwardingAdminTest` were extended with cases for `listConfigResources` covering multiple resource types.
