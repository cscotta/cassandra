**Commit**: 5e7445a6d6 · **Ticket**: KAFKA-17516
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This PR brings client metrics configuration resources in line with the
other config resources in terms of handling synonyms and defaults. Specifically, configs which are not explicitly set take their hard-coded
default values, and these are reported by `kafka-configs.sh --describe`
and `Kafka-client-metrics.sh --describe`. Previously, they were omitted
which means the administrator needed to know the default values. The ConfigHelper was changed so that the handling of client metrics
configuration matches that of group configuration.

**Before/After**
```java
// Before
import scala.collection.mutable.ListBuffer
            val subscriptionName = resource.resourceName
            if (subscriptionName == null || subscriptionName.isEmpty) {
              val entityProps = configRepository.config(new ConfigResource(ConfigResource.Type.CLIENT_METRICS, subscriptionName))
              val configEntries = new ListBuffer[DescribeConfigsResponseData.DescribeConfigsResourceResult]()
              entityProps.forEach((name, value) => {
                configEntries += new DescribeConfigsResponseData.DescribeConfigsResourceResult().setName(name.toString)
                  .setValue(value.toString).setConfigSource(ConfigSource.CLIENT_METRICS_CONFIG.id())
                  .setIsSensitive(false).setReadOnly(false).setSynonyms(List.empty.asJava)
              })

              new DescribeConfigsResponseData.DescribeConfigsResult()

// After
import org.apache.kafka.server.metrics.ClientMetricsConfigs
            if (resource.resourceName == null || resource.resourceName.isEmpty) {
              val clientMetricsProps = configRepository.config(new ConfigResource(ConfigResource.Type.CLIENT_METRICS, resource.resourceName))
              val clientMetricsConfig = ClientMetricsConfigs.fromProps(ClientMetricsConfigs.defaultConfigsMap(), clientMetricsProps)
              createResponseConfig(allConfigs(clientMetricsConfig), createClientMetricsConfigEntry(clientMetricsConfig, clientMetricsProps, includeSynonyms, includeDocumentation))
  private def createGroupConfigEntry(groupConfig: GroupConfig, groupProps: Properties, includeSynonyms: Boolean, includeDocumentation: Boolean)
                                    (name: String, value: Any): DescribeConfigsResponseData.DescribeConfigsResourceResult = {
  private def createClientMetricsConfigEntry(clientMetricsConfig: ClientMetricsConfigs, clientMetricsProps: Properties, includeSynonyms: Boolean, includeDocumentation: Boolean)
                                            (name: String, value: Any): DescribeConfigsResponseData.DescribeConfigsResourceResult = {
    val configEntryType = ClientMetricsConfigs.configType(name).toScala
    val valueAsString = ConfigDef.convertToString(value, configEntryType.orNull)
    val allSynonyms = {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17516 synonyms for client metrics configs (#17264).

**Test**
Test modified in PlaintextAdminIntegrationTest.scala covering the corrected behaviour.
