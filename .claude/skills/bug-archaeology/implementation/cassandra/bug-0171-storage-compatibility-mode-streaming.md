# Bug 0171: storage_compatibility_mode Ignored for Streaming and Tools

**Commit**: d422eb1f35 · **Ticket**: CASSANDRA-19126
**Subsystem**: net/MessagingService, config/DatabaseDescriptor, tools/BulkLoader
**Category**: Wrong constant / default value

---

## Root Cause

`MessagingService.Version.CURRENT` was a `static final` field initialized at class-load time via `DatabaseDescriptor.getStorageCompatibilityMode().isBefore(5)`. Because the field is `static final`, its value is baked in at JVM class initialization. Two problems arose: (1) `DatabaseDescriptor.getStorageCompatibilityMode()` used `conf.storage_compatibility_mode` directly, with `CASSANDRA_4` as the hardcoded field default in `Config.java`, so nodes that did not explicitly set the option in `cassandra.yaml` always defaulted to 4 rather than inheriting the correct mode; (2) tools (e.g., `sstableupgrade`, streaming helpers) called `toolInitialization()` but the `CURRENT` constant was already frozen before `toolInitialization` could set `StorageCompatibilityMode.NONE`, so tools were artificially restricted to messaging version 4 and could not stream data to/from 5.0 nodes.

The fix moves `current_version` from an inner-enum `static final` to a plain `static final int` computed after `DatabaseDescriptor` initialization, introduces a new `applyCompatibilityMode()` step that is called at each init path (server, client, tool), forces tools/clients to `NONE`, and makes `BulkLoader` use `clientInitialization()` instead of `toolInitialization()` so it gets unrestricted streaming.

### Before / After

```java
// BEFORE – frozen at class load, before tool/client init runs
public static final Version CURRENT = DatabaseDescriptor.getStorageCompatibilityMode().isBefore(5) ? VERSION_40 : VERSION_50;
```

```java
// AFTER – computed after initialization, tools get NONE
public static final int current_version = DatabaseDescriptor.getStorageCompatibilityMode().isBefore(5) ? VERSION_40 : VERSION_50;
// accept_streaming initialized in a static block that checks isClientInitialized()
```

---

## How to Spot

`static final` constants that call into subsystems (like `DatabaseDescriptor`) at class initialization time are fragile: they capture the value at whatever state the subsystem is in when the class is first loaded, which may be before the caller's own initialization has configured that subsystem. Look for `static final X = SomeSubsystem.getValue()` patterns and verify that `SomeSubsystem` is guaranteed to be fully initialized before the enclosing class is loaded. Also watch for tool/client init paths that differ from server paths — compatibility-mode logic must be applied at each entry point.

---

## Test

No new test added; existing streaming and compatibility-mode tests exercise the corrected behaviour.
