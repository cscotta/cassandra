## Commit

70c0aca4b7 — KAFKA-17897 [2/N]: listGroups with types filter always requires broker v5, blocking older brokers

## Ticket

KAFKA-17897

## Subsystem

Admin client / ListGroups API versioning

## Category

Wrong constant / default value

## Root cause

`KafkaAdminClient.listGroups(ListGroupsOptions.forConsumerGroups())` always set the `typesFilter` field in the request, which required `ListGroups` v5 (introduced in Kafka 3.8.0). Older brokers rejected the request with `UnsupportedVersionException`. The admin client did not attempt to downgrade the request by omitting the type filter when talking to an older broker. Since classic consumer groups are the only type supported by brokers below v5, listing consumer groups on an older broker does not need a type filter — the response contains only consumer groups by definition. The fix detects `UnsupportedVersionException` on the types-filter field and retries without the filter when the filter only requested classic/consumer groups.

## Before / After

```java
// Before — always sends types filter, throws on brokers < 3.8.0
request.setTypesFilter(List.of("consumer"));  // requires v5

// After — omits types filter for classic/consumer-only requests on older brokers
if (!typesCopy.isEmpty() || (!containedClassic && containedConsumer)) {
    throw new UnsupportedVersionException("need v5 for non-classic types");
}
// else: omit filter, proceed with v4 or lower
```

## How to spot

Search for `ListGroupsRequest` builders that unconditionally set `typesFilter`. Any non-empty types filter requires broker v5; client code must be prepared to omit the filter and accept all groups when talking to older brokers that only support classic groups.

## Test

`KafkaAdminClientTest` was extended with a mock-broker test that simulates a v4 broker and verifies that `listGroups(forConsumerGroups())` succeeds by omitting the types filter.
