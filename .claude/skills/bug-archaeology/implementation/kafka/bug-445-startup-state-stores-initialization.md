**Commit**: 8fa7e095a0 · **Ticket**: KAFKA-19434
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
Instead of creating Standby tasks from the state directory, we open the
local stores that exists in the state directory. This resolves the issue raised in [#KAFKA-19434
](https://issues.apache.org/jira/browse/KAFKA-19434), where
store-specific metrics were being duplicated due to tasks being created
in the main thread and then assigned to a StreamThread. Additionally, since we can now read the offsets from the store during
instance initialization, this clears the way for the implementation of
KIP-1035. As of now, the stores are loading the offsets from the
checkpoint file, but in a later PR, we will read these offsets from the
state store itself.

**Before/After**
```java
// Before
            final List<MetricName> consumerPassedStreamThreadMetricNames = INTERCEPTING_CONSUMERS.get(FIRST_INSTANCE_CLIENT).passedMetrics().stream().map(KafkaMetric::metricName).toList();
            final List<MetricName> adminPassedStreamClientMetricNames = INTERCEPTING_ADMIN_CLIENTS.get(FIRST_INSTANCE_CLIENT).passedMetrics.stream().map(KafkaMetric::metricName).toList();
            // all (alive) threads have received their assignment, close any remaining startup tasks, they're not needed
            stateDirectory.closeStartupTasks();

            log.debug("Initializing STANDBY tasks for existing local state");
            stateDirectory.initializeStartupTasks(topologyMetadata, streamsMetrics, logContext);
    /**
     * Standby tasks initialized for local state on-startup are only partially initialized, because they are not yet
     * assigned to a StreamThread. Once assigned to a StreamThread, we complete their initialization here using the
     * assigned StreamThread's context.
     */

// After
        shouldPassMetrics(topology, FIRST_INSTANCE_CLIENT);
        shouldPassMetrics(topology, SECOND_INSTANCE_CLIENT);
    }

    private void shouldPassMetrics(final Topology topology, final int clientInstance) throws Exception {
            final List<MetricName> consumerPassedStreamThreadMetricNames = INTERCEPTING_CONSUMERS.get(clientInstance).passedMetrics().stream().map(KafkaMetric::metricName).toList();
            final List<MetricName> adminPassedStreamClientMetricNames = INTERCEPTING_ADMIN_CLIENTS.get(clientInstance).passedMetrics.stream().map(KafkaMetric::metricName).toList();
            log.debug("Initializing store offsets for existing local state");
            stateDirectory.initializeStartupStores(topologyMetadata, logContext, streamsMetrics);
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19434: startup state stores initialization (#20749).

**Test**
Test modified in KafkaStreamsTelemetryIntegrationTest.java covering the corrected behaviour.
