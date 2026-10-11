# Bug 1958: THsHaDisruptorServer ignored the configured max Thrift frame size

**Commit**: 19767fcd17 · **Ticket**: CASSANDRA-4573
**Subsystem**: src/java/org/apache/cassandra/thrift/THsHaDisruptorServer.java
**Category**: Wrong constant / default value

---

## Root Cause

When `THsHaDisruptorServer` (the Disruptor-based HsHa Thrift server) constructed its server arguments it never called `.maxFrameSizeInBytes(...)`, so the underlying `thrift-server` library used its own built-in default rather than the value from `cassandra.yaml` (`thrift_framed_transport_size_in_mb`). Clients sending frames larger than that library default would be rejected even when the operator had explicitly raised the limit, and the setting had no effect on this server variant. The fix passes `DatabaseDescriptor.getThriftFramedTransportSize()` to the builder and upgrades the bundled `thrift-server` jar from 0.2.1 to 0.3.0 which exposes that builder method.

### Before / After

```java
// BEFORE
.processor(args.processor);
return new THsHaDisruptorServer(serverArgs);
```

```java
// AFTER
.processor(args.processor)
.maxFrameSizeInBytes(DatabaseDescriptor.getThriftFramedTransportSize());
return new THsHaDisruptorServer(serverArgs);
```

---

## How to Spot

When wrapping a third-party server/transport builder, cross-check every configuration knob exposed by `DatabaseDescriptor` against the builder's available setter methods. An unset builder option silently falls back to the library default, bypassing operator configuration.

---

## Test

No test added.
