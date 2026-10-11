**Commit**: 8b7cf2bc6f · **Ticket**: KAFKA-20129
**Subsystem**: core/src/main/scala/kafka/server/ConfigAdminManager.scala
**Category**: Missing null / bounds check

**Root cause**
In `ConfigAdminManager.prepareIncrementalConfigs`, the helper `listType` called `configKeys(configName)` using Scala's `Map.apply`, which throws a `NoSuchElementException` (not a Kafka API exception) when the key is absent. The caller expected an `InvalidConfigurationException` for unknown config names in APPEND/SUBTRACT operations, so the raw JVM exception escaped the request-handling path instead of being converted to a proper error response.

**Before/After**
```scala
// Before
def listType(configName: String, configKeys: Map[String, ConfigKey]): Boolean = {
  val configKey = configKeys(configName)  // throws NoSuchElementException if missing
  if (configKey == null)
    throw new InvalidConfigurationException(s"Unknown config name: $configName")
  configKey.`type` == ConfigDef.Type.LIST
}

// After
def listType(configName: String, configKeys: Map[String, ConfigKey]): Boolean = {
  configKeys.get(configName) match {
    case Some(configKey) =>
      configKey.`type` == ConfigDef.Type.LIST
    case None =>
      throw new InvalidConfigurationException(s"Unknown config name: $configName")
  }
}
```

**How to spot**
Any path that calls `Map(key)` on a Scala `Map` without a prior `contains` or `.get` check is vulnerable. Look for `map(key)` patterns in request-handler code where unknown input is possible — the implicit assumption that `apply` returns null for missing keys is wrong; Scala maps throw instead.

**Test**
`ConfigAdminManagerTest#testPrepareIncrementalConfigsWithNonExistentKey`: passes a APPEND and a SUBTRACT `AlterConfigOp` referencing a non-existent key against an empty `configKeys` map, then asserts that `InvalidConfigurationException` (not `NoSuchElementException`) is thrown with message "Unknown config name: non.existent.key".
