## Commit

c26b09c609 — KAFKA-18904 [1/N]: Rename ListClientMetricsResources to ListConfigResources

## Ticket

KAFKA-18904

## Subsystem

Admin client / broker protocol (KIP-1142)

## Category

Wrong constant / default value

## Root cause

The internal Kafka protocol API was named `LIST_CLIENT_METRICS_RESOURCES` (API key 74), which encoded a specific resource type in the API name. As KIP-1142 generalised the API to support any `ConfigResource.Type`, the name became misleading and the wire format did not include a `ResourceTypes` field, making it impossible for callers to filter by type without a new API version. The fix renames the API to `LIST_CONFIG_RESOURCES`, adds a `ResourceTypes` tagged field in version 1, and updates all request/response classes and the `ApiKeys` enum entry.

## Before / After

```java
// Before — type baked into the API name, no type filter field
public class ListClientMetricsResourcesRequest extends AbstractRequest { ... }
ApiKeys.LIST_CLIENT_METRICS_RESOURCES  // key 74, max v0

// After — generic name, v1 adds ResourceTypes field
public class ListConfigResourcesRequest extends AbstractRequest { ... }
ApiKeys.LIST_CONFIG_RESOURCES  // key 74, max v1
// RequestData includes: List<String> ResourceTypes (tagged, v1+)
```

## How to spot

When a protocol API name contains a specific resource type (e.g., `ClientMetrics`, `Topics`, `Brokers`) but the underlying storage is generic, future extensibility requires either a new API or a version bump with a type-filter field. Audit any such API when a new resource type is added.

## Test

`ListConfigResourcesRequestTest` was added to verify serialization and deserialization of the new `ResourceTypes` field across API versions.
