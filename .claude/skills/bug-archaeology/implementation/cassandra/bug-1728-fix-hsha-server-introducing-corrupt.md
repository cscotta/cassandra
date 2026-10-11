# Bug 1728: HSHA Server Introducing Corrupt Data

**Commit**: edd6636e05 · **Ticket**: CASSANDRA-6285
**Subsystem**: src/java/org/apache/cassandra/thrift/THsHaDisruptorServer.java
**Category**: Wrong constant / default value

---

## Root Cause

The THsHaDisruptorServer (the Half-Sync/Half-Async Thrift server backed by the LMAX Disruptor) was constructing its `TDisruptorServer.Args` without enabling heap-based allocation. The Disruptor library defaults to off-heap (direct) buffer allocation, but the Thrift transport layer expects heap buffers. When off-heap buffers were fed into the Thrift transport factories, the resulting data corruption occurred because direct `ByteBuffer` contents cannot be reliably accessed after the transport processes them without the proper offset/limit handling that heap buffers provide.

### Before / After

```java
// BEFORE
TDisruptorServer.Args serverArgs = new TDisruptorServer.Args(serverTransport)
    .inputTransportFactory(args.inTransportFactory)
```

```java
// AFTER
TDisruptorServer.Args serverArgs = new TDisruptorServer.Args(serverTransport)
    .useHeapBasedAllocation(true)
    .inputTransportFactory(args.inTransportFactory)
```

---

## How to Spot

When constructing a third-party server/framework object and immediately chaining configuration, check whether the library's default allocation or buffer strategy is compatible with the surrounding code. Missing a single flag on a builder chain is easy to overlook, especially when the option name (`useHeapBasedAllocation`) does not appear prominently in examples.

---

## Test

No test added.
