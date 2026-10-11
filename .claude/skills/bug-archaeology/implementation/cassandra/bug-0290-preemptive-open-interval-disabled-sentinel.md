# Bug 0290: sstable_preemptive_open_interval_in_mb=-1 Not Treated as Disabled

**Commit**: fe28f482db · **Ticket**: CASSANDRA-17737
**Subsystem**: config / sstable
**Category**: Wrong constant / default value

---

## Root Cause

When Cassandra 4.1 introduced typed configuration parameters (with unit suffixes), the old integer field `sstable_preemptive_open_interval_in_mb` used `-1` as the sentinel value meaning "disabled". The new field `sstable_preemptive_open_interval` uses `null` for the same purpose. The converter `MEBIBYTES_DATA_STORAGE_INT` did not handle the negative-to-null mapping, so loading an old config with `-1` would attempt to construct a `DataStorageSpec.IntMebibytesBound(-1)`, which threw an `IllegalArgumentException` instead of mapping to a disabled state. Conversely, `unconvert()` called `toMebibytes()` on a `null` value, causing a `NullPointerException` in the Settings virtual table.

### Before / After

```java
// BEFORE - converter had no negative-sentinel handling
MEBIBYTES_DATA_STORAGE_INT(Integer.class, DataStorageSpec.IntMebibytesBound.class,
                           DataStorageSpec.IntMebibytesBound::new,
                           DataStorageSpec.IntMebibytesBound::toMebibytes),
```

```java
// AFTER - new converter handles negative == disabled
NEGATIVE_MEBIBYTES_DATA_STORAGE_INT(Integer.class, DataStorageSpec.IntMebibytesBound.class,
                                    o -> o < 0 ? null : new DataStorageSpec.IntMebibytesBound(o),
                                    o -> o == null ? -1 : o.toMebibytes()),
```

---

## How to Spot

Every converter that bridges a legacy integer sentinel (typically `-1` or `0`) to a new nullable typed parameter must implement both directions of the mapping: the forward direction (integer → typed, with sentinel → null) and the reverse direction (null → sentinel integer). During review, check that `@Replaces` converters for nullable fields use a converter that explicitly handles `null` in `unconvert()`, and that the `Config` field is annotated `@Nullable` so the YAML loader does not treat a null assignment as an error.

---

## Test

`YamlConfigurationLoaderTest.readConvertersSpecialCasesFromConfig()` verifies that both `sstable_preemptive_open_interval: null` (new style) and `sstable_preemptive_open_interval_in_mb: -1` (old style) load as `null`; `SettingsTableTest.virtualTableBackwardCompatibility()` verifies the virtual table round-trips the disabled sentinel correctly.
