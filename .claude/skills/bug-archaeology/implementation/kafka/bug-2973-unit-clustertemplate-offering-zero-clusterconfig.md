**Commit**: db2a09fa90 · **Ticket**: KAFKA-16652
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    void testProcessClusterTemplate() {
        ExtensionContext context = mock(ExtensionContext.class);
        when(annot.value()).thenReturn("").thenReturn(" ");
        Assertions.assertThrows(IllegalStateException.class, () ->
                ext.processClusterTemplate(context, annot)
        Assertions.assertThrows(IllegalStateException.class, () ->
                ext.processClusterTemplate(context, annot)

// After
import kafka.test.ClusterConfig;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;


    static List<ClusterConfig> cfgEmpty() {
        return Collections.emptyList();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16652 add unit test for clustertemplate offering zero clusterconfig (#15862).

**Test**
Test modified in ClusterTestExtensionsUnitTest.java covering the corrected behaviour.
