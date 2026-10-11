**Commit**: c04b2d9920 · **Ticket**: KAFKA-20289
**Subsystem**: core — ConfigHelper / DescribeConfigs / GroupConfig
**Category**: Wrong constant / default value

**Root cause**
`ConfigHelper.createGroupConfigEntry` looked up broker synonyms by passing the *group config name* (e.g., `consumer.session.timeout.ms`) directly to `brokerSynonyms()`, which expected a *broker config name* (e.g., `group.consumer.session.timeout.ms`). The two namespaces use different key strings, so the lookup found no synonyms or wrong ones, producing an incorrect/empty synonym chain in `DescribeConfigs` responses for group configs. The fix introduces an explicit `GroupConfig.brokerSynonym(name)` mapping so each group config key is translated to its corresponding broker config key before the synonym lookup.

**Before/After**
```scala
// Before — group config name passed directly to broker synonym lookup
val allNames = brokerSynonyms(name)   // name is a group config key, not broker key
val list = configSynonyms(name, allNames, isSensitive)

// After — translate to broker synonym first
val list = GroupConfig.brokerSynonym(name).toScala match {
  case Some(brokerName) =>
    configSynonyms(brokerName, brokerSynonyms(brokerName), isSensitive)
  case None =>
    // No broker synonym: use default only
    ...
}
```

**How to spot**
Whenever a "describe" API has to merge configs from multiple scopes (group, broker dynamic, broker static, default), each scope uses different key names. Passing the wrong namespace key into `brokerSynonyms()` silently produces an empty list rather than an error. Review any config synonym lookup to confirm the key is translated to the correct namespace before being passed.

**Test**
`DynamicGroupConfigIntegrationTest` — new integration test verifies the full synonym chain (`DYNAMIC_GROUP_CONFIG → DYNAMIC_BROKER_CONFIG → DYNAMIC_DEFAULT_BROKER_CONFIG → DEFAULT_CONFIG`) for a config with a broker synonym, and a separate test verifies a config without a broker synonym shows only `DYNAMIC_GROUP_CONFIG → DEFAULT_CONFIG`.
