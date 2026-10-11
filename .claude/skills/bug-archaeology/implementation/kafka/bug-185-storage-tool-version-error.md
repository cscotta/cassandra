**Commit**: 9572d19c59 · **Ticket**: KAFKA-19509
**Subsystem**: core/src/main/scala/kafka/tools/StorageTool.scala
**Category**: Wrong constant / default value

**Root cause**: When an unknown `--release-version` value was passed to `kafka-storage.sh format`, `MetadataVersion.fromVersionString` threw an uncaught exception that propagated as a raw stack trace to the user. The error message gave no indication of what values are valid. The fix wraps the call in a try/catch and throws a `TerseFailure` with a human-readable list of the supported metadata versions (from `MINIMUM_VERSION` to `latestProduction()`).

**Before/After**:
```scala
// BEFORE – raw exception propagates with no useful message
Option(namespace.getString("release_version")).foreach(
  releaseVersion => formatter.setReleaseVersion(
    MetadataVersion.fromVersionString(releaseVersion)))

// AFTER – catches the exception and surfaces valid versions
Option(namespace.getString("release_version")).foreach(releaseVersion => {
  try {
    formatter.setReleaseVersion(MetadataVersion.fromVersionString(releaseVersion))
  } catch {
    case _: Throwable =>
      throw new TerseFailure(s"Unknown metadata.version $releaseVersion. " +
        s"Supported metadata.version are ${metadataVersionsToString(...)}")
  }
})
```

**How to spot**: CLI tools that call any `fromString`/`fromVersionString` helper without a surrounding try/catch will surface confusing exception messages to operators. Search for `fromVersionString` (or similar parsers) in tool entry points that are not wrapped.

**Test**: `StorageToolTest.testFormatWithUnsupportedReleaseVersion` calls `runFormatCommand` with `--release-version 3.3-IV1`, expects `TerseFailure`, and asserts the message contains `"Unknown metadata.version 3.3-IV1"` and both `MINIMUM_VERSION` and `latestProduction()` version strings.
