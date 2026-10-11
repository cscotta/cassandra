# Bug 0830: cassandra-stress print-settings output has wrong labels

**Commit**: 11aeeea2cb · **Ticket**: CASSANDRA-12473
**Subsystem**: cassandra-stress / settings display
**Category**: Wrong constant / default value

---

## Root Cause
Three independent copy-paste / transposition errors corrupted the human-readable settings output of cassandra-stress. First, `OptionDistribution.GaussianFactory.getConfigAsString()` had `stdev` and `mean` swapped in the format-string argument list, so the printed values were in the wrong order. Second, `OptionRatioDistribution.getConfigAsString()` misspelled `"Ratio"` as `"Ration"`. Third, `SettingsLog.printSettings()` printed the label `"Print Setting"` for the `noSettings` field instead of `"No Settings"`, making the output semantically incorrect.

### Before / After
```java
// BEFORE — mean and stdev swapped
String.format("Gaussian:  min=%d,max=%d,mean=%f,stdev=%f", min, max, stdev, mean)

// AFTER — correct order
String.format("Gaussian:  min=%d,max=%d,mean=%f,stdev=%f", min, max, mean, stdev)

// BEFORE — typo
String.format("Ration: divisor=%f;delegate=%s", ...)
// AFTER
String.format("Ratio: divisor=%f;delegate=%s", ...)

// BEFORE — wrong label
out.printf("  Print Setting: %b%n", noSettings);
// AFTER
out.printf("  No Settings: %b%n", noSettings);
```

---

## How to Spot
Format-string argument order bugs are invisible to the compiler — only a test that checks the actual printed text can catch them. Any `String.format` call where two adjacent parameters have the same type (two `double` values, two `boolean` flags) is susceptible. A quick unit test that invokes `getConfigAsString()` on a known distribution and asserts specific substrings would have caught all three defects.

---

## Test
No new test added.
