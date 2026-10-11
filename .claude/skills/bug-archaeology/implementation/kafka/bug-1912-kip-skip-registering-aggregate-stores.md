**Commit**: 31d97bc3c9 · **Ticket**: KAFKA-18026
**Subsystem**: streams/src
**Category**: Incorrect filtering / wrong query result

**Root cause**
Minor followup to #17929 based on this discussion

Also includes some very minor refactoring/renaming on the side. The only real change is in the KGroupedStreamImpl class

Reviewers: Guozhang Wang <guozhang.wang.us@gmail.com>

**Before/After**
```java
// Before
        processRepartitions(groupPatterns, storeFactory.name());
        return createTable(processors, parentProcessors, named, keySerde, valueSerde, queryableName, storeFactory.name());
        processRepartitions(groupPatterns, storeFactory.name());
        return createTable(processors, parentProcessors, named, keySerde, valueSerde, queryableName, storeFactory.name());
        processRepartitions(groupPatterns, storeFactory.name());
        return createTable(processors, parentProcessors, named, keySerde, valueSerde, queryableName, storeFactory.name());
        processRepartitions(groupPatterns, storeFactory.name());
        return createTable(processors, parentProcessors, named, keySerde, valueSerde, queryableName, storeFactory.name());
                    new String[]{storeFactory.name()}

// After
        processRepartitions(groupPatterns, storeFactory.storeName());
        return createTable(processors, parentProcessors, named, keySerde, valueSerde, queryableName, storeFactory.storeName());
        processRepartitions(groupPatterns, storeFactory.storeName());
        return createTable(processors, parentProcessors, named, keySerde, valueSerde, queryableName, storeFactory.storeName());
        processRepartitions(groupPatterns, storeFactory.storeName());
        return createTable(processors, parentProcessors, named, keySerde, valueSerde, queryableName, storeFactory.storeName());
        processRepartitions(groupPatterns, storeFactory.storeName());
        return createTable(processors, parentProcessors, named, keySerde, valueSerde, queryableName, storeFactory.storeName());
                    new String[]{storeFactory.storeName()}
```

**How to spot**
When reviewing streams/src code, look for incorrect filtering / wrong query result patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18026: kip-1112, skip re-registering aggregate stores in statefulprocessornode (#18015).

**Test**
Test modified in StateStoreNode.java covering the corrected behaviour.
