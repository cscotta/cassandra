**Commit**: 0109a3f718 · **Ticket**: KAFKA-15045
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
Fixed the calculation of the store name list based on the subtopology being accessed. Also added a new test to make sure this new functionality works as intended. Reviewers: Anna Sophie Blee-Goldman <ableegoldman@apache.org>

**Before/After**
```java
// Before
              files="(KafkaStreams|KStreamImpl|KTableImpl).java"/>
        final Set<String> nodeGroup = new HashSet<>();
            nodeGroup.addAll(value);
        nodeGroup.removeAll(globalNodeGroups());
        return build(nodeGroup);
                    .stateStoreNameToSourceTopicsForTopology(taskId.topologyName())
                    .keySet();

// After
              files="(InternalTopologyBuilder|KafkaStreams|KStreamImpl|KTableImpl).java"/>
    // keyed by subtopology id
    // keyed by subtopology id
    private Map<Integer, Set<String>> subtopologyIdToStateStoreNames = null;

        final Set<String> allNodes = new HashSet<>();
            allNodes.addAll(value);
        allNodes.removeAll(globalNodeGroups());
        initializeSubtopologyIdToStateStoreNamesMap();
        return build(allNodes);
    public Set<String> stateStoreNamesForSubtopology(final int subtopologyId) {
        return subtopologyIdToStateStoreNames.get(subtopologyId);
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15045: (kip-924 pt. 17) state store computation fixed (#16194).

**Test**
Test modified in InternalTopologyBuilderTest.java covering the corrected behaviour.
