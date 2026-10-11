# Bug 0280: BulkLoader Missing entireSSTable Throttle CLI Options

**Commit**: 83c169ec9e · **Ticket**: CASSANDRA-17677
**Subsystem**: tools/BulkLoader, config/DatabaseDescriptor
**Category**: Wrong constant / default value

---

## Root Cause

`BulkLoader` applied the streaming throttle by calling `DatabaseDescriptor.setStreamThroughputOutboundMegabitsPerSec()`, which converts a megabits-per-second value. However the CLI option values in `LoaderOptions` were already in MiB/s, so the unit conversion was wrong (values were silently treated as Mbits instead of MiB). More critically, the two "entire SSTable" throttle settings (`entireSSTableThrottle` and `entireSSTableInterDcThrottle`) were parsed from `LoaderOptions` but never applied to `DatabaseDescriptor` at all, so the options had zero effect on actual streaming behaviour.

### Before / After

```java
// BEFORE
DatabaseDescriptor.setStreamThroughputOutboundMegabitsPerSec(options.throttle);
DatabaseDescriptor.setInterDCStreamThroughputOutboundMegabitsPerSec(options.interDcThrottle);
// entireSSTableThrottle* never applied
```

```java
// AFTER
DatabaseDescriptor.setStreamThroughputOutboundBytesPerSec(options.throttleBytes);
DatabaseDescriptor.setInterDCStreamThroughputOutboundBytesPerSec(options.interDcThrottleBytes);
DatabaseDescriptor.setEntireSSTableStreamThroughputOutboundMebibytesPerSec(options.entireSSTableThrottleMebibytes);
DatabaseDescriptor.setEntireSSTableInterDCStreamThroughputOutboundMebibytesPerSec(options.entireSSTableInterDcThrottleMebibytes);
```

---

## How to Spot

When a CLI tool parses options into fields but those fields are then passed to a different setter than expected (or not passed at all), the wiring between option parsing and configuration application should be audited end-to-end. Adding new throttle knobs to `DatabaseDescriptor` without also updating every tool that configures streaming is a common omission; a grep for callers of the old setter is the most direct check.

---

## Test

`BulkLoaderTest` and `LoaderOptionsTest` verify that the entire-SSTable throttle CLI flags are parsed and that the correct `DatabaseDescriptor` setters are called with the expected byte-per-second values.
