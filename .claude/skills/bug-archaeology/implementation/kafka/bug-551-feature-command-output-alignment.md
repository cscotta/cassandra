**Commit**: 33df6535ed · **Ticket**: KAFKA-19941

**Subsystem**: tools/src/main/java/org/apache/kafka/tools/FeatureCommand.java

**Category**: Wrong constant/default value

**Root cause**
`FeatureCommand.java` printed feature output using fixed tab characters (`\t`) as separators, causing columns to mis-align whenever feature or version strings differed in length. There was no minimum field width, so a long feature name would push subsequent columns arbitrarily far right. The fix replaced tab separators with left-aligned, padded format specifiers (`%-40s`, `%-15s`, etc.) so every row lines up regardless of content width.

**Before**
```java
// Before
System.out.printf("Feature: %s\tSupportedMinVersion: %s\t" +
    "SupportedMaxVersion: %s\tFinalizedVersionLevel: %s\t" +
    "Epoch: %s", feature, minVer, maxVer, finalVer, epoch);
```

**After**
```java
// After
System.out.printf("%-40s  %-15s  %-15s  %-20s  %s%n",
    "Feature: " + feature,
    "SupportedMinVersion: " + minVer,
    "SupportedMaxVersion: " + maxVer,
    "FinalizedVersionLevel: " + finalVer,
    "Epoch: " + epoch);
```

**How to spot**
Run `kafka-features.sh --describe` against a cluster with several features and look at the output: if columns shift sideways row-to-row the old tab-based format is in use. Any `printf` in a CLI tool that uses `\t` for multi-column alignment rather than a fixed-width format specifier is a candidate for this class of cosmetic misalignment.

**Test**
Visually compare `kafka-features.sh --describe` output before and after the patch to confirm all rows align into straight columns.
