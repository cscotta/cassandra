# Bug 1202: Wrong Default Value for row-population-ratio in cassandra-stress

**Commit**: f708c1e416 · **Ticket**: n/a
**Subsystem**: tools/stress/src/org/apache/cassandra/stress/settings/OptionRatioDistribution.java
**Category**: Wrong constant / default value

---

## Root Cause

`OptionRatioDistribution` parsed a default spec by constructing a new instance with `delegate.prefix` as the prefix string. When the delegate prefix contained a non-empty option name (e.g. `"row-population-ratio="`) the `accept()` call on the temporary sub-object would fail to match the bare default spec string (e.g. `"fixed(1)/1"`), causing no default to be applied and leaving the ratio undefined. The fix passes an empty string `""` as the prefix for the temporary instance so the bare spec is accepted correctly. Separately, `SettingsInsert` had `rowPopulationRatio` declared without a default; the fix adds `"fixed(1)/1"` so omitting the option gives a sensible 100% population ratio.

### Before / After

```java
// BEFORE
OptionRatioDistribution sub = new OptionRatioDistribution(delegate.prefix, null, null, true);

final OptionRatioDistribution rowPopulationRatio =
    new OptionRatioDistribution("row-population-ratio=", null,
        "The percent of a given rows columns to populate", false);
```

```java
// AFTER
OptionRatioDistribution sub = new OptionRatioDistribution("", null, null, true);

final OptionRatioDistribution rowPopulationRatio =
    new OptionRatioDistribution("row-population-ratio=", "fixed(1)/1",
        "The percent of a given rows columns to populate", false);
```

---

## How to Spot

When a utility class that parses prefixed key=value options is reused to parse a bare default spec, ensure the prefix is stripped or set to empty before calling `accept()`. Any `OptionXxx` or `SettingsXxx` class whose default-spec path instantiates a temporary helper object should pass `""` (or an analogous empty sentinel) rather than copying the live prefix, which contaminates the parse.

---

## Test

No test added.
