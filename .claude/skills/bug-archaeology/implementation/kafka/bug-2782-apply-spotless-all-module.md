**Commit**: 1040d78372 · **Ticket**: KAFKA-10787
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
def excludedSpotlessModules = [':streams']
  if ((JavaVersion.current().isJava11() || (JavaVersion.current() == JavaVersion.VERSION_17)) && project.path !in excludedSpotlessModules) {
import java.util.Arrays;
import java.util.HashSet;



import static org.apache.kafka.streams.StreamsConfig.DSL_STORE_SUPPLIERS_CLASS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DSL_STORE_SUPPLIERS_CLASS_DEFAULT;
import static org.apache.kafka.streams.StreamsConfig.DSL_STORE_SUPPLIERS_CLASS_DOC;
import static org.apache.kafka.streams.StreamsConfig.STATESTORE_CACHE_MAX_BYTES_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.STATESTORE_CACHE_MAX_BYTES_DOC;

// After
  if ((JavaVersion.current().isJava11() || (JavaVersion.current() == JavaVersion.VERSION_17))) {


import java.util.Arrays;
import java.util.HashSet;


import static org.apache.kafka.streams.StreamsConfig.DEFAULT_DSL_STORE_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_DSL_STORE_DOC;
import static org.apache.kafka.streams.StreamsConfig.DSL_STORE_SUPPLIERS_CLASS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DSL_STORE_SUPPLIERS_CLASS_DEFAULT;
import static org.apache.kafka.streams.StreamsConfig.DSL_STORE_SUPPLIERS_CLAS
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-10787 apply spotless to all module (#16467).

**Test**
Test modified in KafkaStreamsTest.java covering the corrected behaviour.
