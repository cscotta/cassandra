# Bug 0987: Inter-DC Stream Throughput Default Was Unlimited

**Commit**: 6d6d189042 · **Ticket**: CASSANDRA-9708
**Subsystem**: Streaming / Configuration
**Category**: Wrong constant / default value

---

## Root Cause
`Config.inter_dc_stream_throughput_outbound_megabits_per_sec` was initialised to `0`, which the streaming code interpreted as "no throttle — unlimited". The intra-DC default was already 200 Mbps. Having the inter-DC setting default to unlimited was unintentional, and in practice it allowed a single streaming session to saturate an entire WAN link. The fix sets the default to 200 Mbps (matching the intra-DC default) and exposes `getinterdcstreamthroughput` / `setinterdcstreamthroughput` nodetool commands and an `--inter-dc-throttle` BulkLoader flag so operators can observe and adjust it at runtime.

### Before / After
```java
// BEFORE
public volatile Integer inter_dc_stream_throughput_outbound_megabits_per_sec = 0;

// AFTER
public volatile Integer inter_dc_stream_throughput_outbound_megabits_per_sec = 200;
```

---

## How to Spot
Any throttle or rate-limiting configuration field initialised to `0` where `0` means "disabled" should be cross-checked against its intra-equivalent. A conscious "unlimited by default" choice should be documented with a comment; an absent comment is a signal that the `0` is accidental.

---

## Test
No test added.
