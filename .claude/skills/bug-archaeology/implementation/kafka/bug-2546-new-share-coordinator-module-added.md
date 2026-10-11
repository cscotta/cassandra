**Commit**: 4928b549fa · **Ticket**: KAFKA-17330
**Subsystem**: build.gradle
**Category**: Wrong serialization / deserialization

**Root cause**
What
Introduced a new module share-coordinator to house relevant implementation code and resources. Modified settings.gradle and build.gradle to accommodate the module. Added ShareSnapshot[Key, Value], ShareUpdate[Key, Value] message record schemas. Introduced a trivial impl of ShareCoordinatorShard class to establish dependencies with other modules (:coordinator-common, :metadata).

**Before/After**
```java
// Before
// (no removals)

// After
project(':share-coordinator') {
  base {
    archivesName = "kafka-share-coordinator"
  }

  configurations {
    generator
  }

  dependencies {
    implementation libs.jacksonDatabind
    implementation project(':clients')
```

**How to spot**
When reviewing build.gradle code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17330: new share-coordinator module. added record schemas. (#16877).

**Test**
No test added.
