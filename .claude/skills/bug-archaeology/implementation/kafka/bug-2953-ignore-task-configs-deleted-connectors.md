**Commit**: 0409003c43 · **Ticket**: KAFKA-16837
**Subsystem**: connect/runtime
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
    private final Logger log = LoggerFactory.getLogger(AbstractHerder.class);
    public boolean taskConfigsChanged(ClusterConfigState configState, String connName, List<Map<String, String>> taskProps) {
        if (taskProps.size() != currentNumTasks) {
            log.debug("Connector {} task count changed from {} to {}", connName, currentNumTasks, taskProps.size());
                if (!taskProps.get(index).equals(configState.taskConfig(taskId))) {
        if (!taskConfigsChanged(configState, connName, taskProps)) {
        List<Map<String, String>> rawTaskProps = reverseTransform(connName, configState, taskProps);
        if (taskConfigsChanged(configState, connName, newTaskConfigs)) {
            List<Map<String, String>> rawTaskConfigs = reverseTransform(connName, configState, newTaskConfigs);
                connectorConfigs.remove(connectorName);
                connectorTaskCounts.remove(connectorName);
                taskConfigs.keySet().removeIf(taskId -> taskId.connector().equals(connectorName));

// After
    private static final Logger log = LoggerFactory.getLogger(AbstractHerder.class);
    public static boolean taskConfigsChanged(ClusterConfigState configState, String connName, List<Map<String, String>> rawTaskProps) {
        if (rawTaskProps.size() != currentNumTasks) {
            log.debug("Connector {} task count changed from {} to {}", connName, currentNumTasks, rawTaskProps.size());
                if (!rawTaskProps.get(index).equals(configState.rawTaskConfig(taskId))) {
        List<Map<String, String>> rawTaskProps = reverseTransform(connName, configState, taskProps);
        if (!taskConfigsChanged(configState, connName, rawTaskProps)) {
        List<Map<String, String>> rawTaskConfigs = reverseTransform(connName, configState, newTaskConfigs);
        if (taskConfigsChanged(configState, connName, rawTaskConfigs)) {
                processConnectorRemoval(connectorName);
```

**How to spot**
When reviewing connect/runtime code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16837, kafka-16838: ignore task configs for deleted connectors, and compare raw task configs before publishing the.

**Test**
Test modified in ConnectWorkerIntegrationTest.java covering the corrected behaviour.
