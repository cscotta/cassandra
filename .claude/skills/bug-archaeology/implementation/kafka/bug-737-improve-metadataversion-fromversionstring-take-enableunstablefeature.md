**Commit**: ac63ce9789 · **Ticket**: KAFKA-19544
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Improve `MetadataVersion.fromVersionString()` to take an
`enableUnstableFeature` flag,   and enable `FeatureCommand` and
`StorageTool` to leverage the exception message from
`fromVersionString`. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    def metadataVersionsToString(first: MetadataVersion, last: MetadataVersion): String = {
      val versions = MetadataVersion.VERSIONS.slice(first.ordinal, last.ordinal + 1)
      versions.map(_.toString).mkString(", ")
    }
        formatter.setReleaseVersion(MetadataVersion.fromVersionString(releaseVersion))
        case _: Throwable =>
          throw new TerseFailure(s"Unknown metadata.version $releaseVersion. Supported metadata.version are " +
            s"${metadataVersionsToString(MetadataVersion.MINIMUM_VERSION, MetadataVersion.latestProduction())}")
   * @param namespace       Arguments containing the release version.
   * @param printStream     The print stream to output the version mapping.
   * @param validFeatures   List of features to be considered in the output
      val metadataVersion = MetadataVersion.fromVersionString(releaseVersion)

// After
        formatter.setReleaseVersion(MetadataVersion.fromVersionString(releaseVersion, config.unstableFeatureVersionsEnabled))
        case e: Throwable =>
          throw new TerseFailure(e.getMessage)
   * @param namespace                     Arguments containing the release version.
   * @param printStream                   The print stream to output the version mapping.
   * @param validFeatures                 List of features to be considered in the output.
      val metadataVersion = MetadataVersion.fromVersionString(releaseVersion, true)
        throw new TerseFailure(e.getMessage)
        setBootstrapMetadataVersion(MetadataVersion.fromVersionString(metadataVersionString, true)).
    assertTrue(failure.contains("Unknown metadata.version '3.3-IV1'"))
    assertEquals("Unknown metadata.version '2.9-IV2'. Supported metadata.version are: " +
      MetadataVersion.metadataVersionsToString(MetadataVersion.MINIMUM_VERSION, MetadataVersion.latestTesting()),
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19544 improve `metadataversion.fromversionstring()` to take an enableunstablefeature flag (#20248).

**Test**
Test modified in KRaftClusterTest.scala covering the corrected behaviour.
