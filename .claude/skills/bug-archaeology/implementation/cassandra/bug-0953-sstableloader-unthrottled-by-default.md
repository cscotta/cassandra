# Bug 0953: sstableloader streams throttled by default

**Commit**: 434dfc97a7 · **Ticket**: CASSANDRA-9714
**Subsystem**: Tools / Streaming
**Category**: Wrong constant / default value

---

## Root Cause
When `sstableloader` ran without a configuration file it constructed a `new Config()`, which left `stream_throughput_outbound_megabits_per_sec` and `inter_dc_stream_throughput_outbound_megabits_per_sec` at their Config-class defaults — both non-zero. The result was that bulk-load streaming was artificially throttled even though sstableloader is a batch tool expected to use full available bandwidth. The fix explicitly sets both throttle fields to `0` (unlimited) in the code path that runs without a config file.

### Before / After
```java
// BEFORE
else
{
    config = new Config();
    // throttle fields left at non-zero Config defaults
}

// AFTER
else
{
    config = new Config();
    // unthrottle stream by default
    config.stream_throughput_outbound_megabits_per_sec = 0;
    config.inter_dc_stream_throughput_outbound_megabits_per_sec = 0;
}
```

---

## How to Spot
Any time a tool constructs a `Config` object inline (rather than loading `cassandra.yaml`) and later passes it to the streaming subsystem, check whether the default numeric fields match the semantically expected behaviour for that tool. A throughput field defaulting to a class-level constant may differ from the intended "unlimited" sentinel value of `0`. Review the no-config branch of `ExternalClient.init()` / `BulkLoader.CmdLineOptions` path to verify both intra- and inter-DC throttle fields are set explicitly.

---

## Test
No test added.
