## Commit
58b56c213e

## Ticket
KAFKA-15853

## Subsystem
Broker / Config

## Category
Logic error in condition

## Root cause
Static utility methods (`configType`, `loggableValue`, `maybeSensitive`) lived in `KafkaConfig.scala` but were needed by Java callers in `AbstractKafkaConfig`. Scala's static forwarders are unreliable from Java when subclassing, causing `NoSuchMethodError` at runtime. Moving them to `AbstractKafkaConfig` as proper Java static methods fixes the cross-language visibility. Additionally `maxConnectionsPerIpOverrides` returned a Scala `Map` instead of `java.util.Map`, breaking Java consumers.

## Before
```scala
// KafkaConfig.scala companion object
def configType(configName: String): Option[ConfigDef.Type] = ...
def loggableValue(resourceType: ConfigResource.Type, ...): String = ...
```

## After
```java
// AbstractKafkaConfig.java
public static Optional<ConfigDef.Type> configType(String configName) { ... }
public static String loggableValue(ConfigResource.Type resourceType, ...) { ... }
```

## How to spot
When moving Scala companion-object methods to a Java base class, always check that all call sites (especially Java ones) use the Java version. Search for `KafkaConfig.configType(` and `KafkaConfig.loggableValue(` in Java files to find remaining callers.

## Test
`KafkaConfigTest` exercises the moved methods via the Java API.
