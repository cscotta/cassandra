**Commit**: c3f13b5c57 · **Ticket**: KAFKA-16308
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
I've added the release-version flag to the upgrade and downgrade commands. I've also added tests. While working on this, I realized that we reveal non-production features to be returned in the version-mapping and dependencies commands. I have changed this to only return production features (except in tests) and added tests for this.

**Before/After**
```java
// Before
        runVersionMappingCommand(namespace, printStream)
        runFeatureDependenciesCommand(namespace, printStream)
    printStream: PrintStream
      for (feature <- Features.values()) {
    printStream: PrintStream
    if (featureArgs != null) {
      for (featureArg <- featureArgs) {
        val Array(featureName, versionStr) = featureArg.split("=")
        val featureLevel = try {
          versionStr.toShort
          case _: NumberFormatException =>
            throw new TerseFailure(s"Invalid version format: $versionStr for feature $featureName")

// After
        runVersionMappingCommand(namespace, printStream, Features.PRODUCTION_FEATURES)
        runFeatureDependenciesCommand(namespace, printStream, Features.PRODUCTION_FEATURES)
   * @param validFeatures   List of features to be considered in the output
    printStream: PrintStream,
    validFeatures: java.util.List[Features]
      for (feature <- validFeatures.asScala) {
    printStream: PrintStream,
    validFeatures: java.util.List[Features]
    for (featureArg <- featureArgs) {
      val Array(featureName, versionStr) = featureArg.split("=")

      val featureLevel = try {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16308 [4/4]: add release-version flag to upgrade and downgrade commands (#17362).

**Test**
Test modified in StorageToolTest.scala covering the corrected behaviour.
