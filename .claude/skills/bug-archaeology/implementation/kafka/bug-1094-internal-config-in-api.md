## Commit

3696c49788 — KAFKA-19220: Add tests to ensure internal configs are not returned by public APIs

## Ticket

KAFKA-19220

## Subsystem

Admin client / broker configuration API

## Category

Incorrect filtering / wrong query result

## Root cause

Internal broker configurations (those marked with `ConfigDef.Importance.LOW` and an internal prefix) were being returned by `createTopics` and `describeConfigs` API responses when callers did not explicitly filter them. This exposed internal tuning parameters to external clients, which could cause problems in client code that iterated over returned configurations and applied them blindly. No regression test existed to catch future additions of internal configs that might accidentally be surfaced.

## Before / After

```java
// Before — no assertion that internal configs are absent from API response
Map<String, ConfigEntry> configs = adminClient.createTopics(...).config(topicName).get();
// could silently include internal configs

// After — explicit assertion that no internal config is returned
Map<String, ConfigEntry> configs = adminClient.createTopics(...).config(topicName).get();
configs.values().forEach(e ->
    assertFalse(e.isInternal(), "Internal config " + e.name() + " must not be exposed"));
```

## How to spot

In `createTopics` and `describeConfigs` response-processing tests, add assertions that no entry in the returned config map has `isInternal() == true`. The absence of this assertion allows future internal-config additions to accidentally appear in public API responses without CI catching it.

## Test

New `StaticBrokerConfigTest.java` covers both `createTopics` and `describeConfigs` to assert that internal configurations are excluded from the response.
