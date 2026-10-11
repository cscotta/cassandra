# Bug 0092: nowInSec Deserialization and Mutation serializedSize Under-Count

**Commit**: a9cb71833a · **Ticket**: CASSANDRA-19023
**Subsystem**: db — Mutation; service/accord — AccordService (live migration / interop)
**Category**: Wrong serialization / deserialization

---

## Root Cause

This squash commit contains several distinct fixes. The two most impactful serialization bugs are: (1) `nowInSec` was deserialized with the wrong type or path after the interop rebase, producing incorrect timestamps in range-read commands; and (2) `Mutation.Serializer.serializedSize` for version >= VERSION_51 added the raw integer constant `ALLOW_OUT_OF_RANGE_MUTATIONS_FLAG` (value `1`) as the size contribution for the flags byte instead of calling `TypeSizes.sizeof((byte)ALLOW_OUT_OF_RANGE_MUTATIONS_FLAG)` (also `1`, but semantically correct and consistent). Beyond serialization, `AccordService.startup()` was not synchronized, creating a race where `isSetup()` could return `true` before the service was fully initialized, and the `AccordService` singleton used an unsafe lazy-holder pattern that allocated the service regardless of whether Accord transactions were enabled. The Accord repair path also used raw `Clock.Global.nanoTime()` instead of the injectable `ctx.clock().nanoTime()`, breaking determinism in tests.

### Before / After

```java
// BEFORE — serializedSize counts flag as raw int value, not byte size
if (version >= VERSION_51)
    size += ALLOW_OUT_OF_RANGE_MUTATIONS_FLAG; // adds 1 as int literal
```

```java
// AFTER
if (version >= VERSION_51)
    size += TypeSizes.sizeof((byte)ALLOW_OUT_OF_RANGE_MUTATIONS_FLAG); // correct byte sizing
```

```java
// BEFORE — AccordService initialized unconditionally via static holder
private static class Handle {
    public static final AccordService instance = new AccordService(); // created even if disabled
}
public static IAccordService instance() {
    return DatabaseDescriptor.getAccordTransactionsEnabled() ? Handle.instance : NOOP_SERVICE;
}
```

```java
// AFTER — initialized only on startup()
public synchronized static void startup(NodeId tcmId) {
    if (!DatabaseDescriptor.getAccordTransactionsEnabled()) { instance = NOOP_SERVICE; return; }
    AccordService as = new AccordService(AccordTopologyUtils.tcmIdToAccord(tcmId));
    as.startup();
    instance = as;
}
```

---

## How to Spot

Wherever `serializedSize` and `serialize` are implemented together, verify that every field present in `serialize` is also counted in `serializedSize` using the exact same size expression (e.g., `TypeSizes.sizeof(byte)` not a bare integer literal). Discrepancies cause buffer over- or under-allocation that is only caught at runtime. For service singletons, review static initializer blocks and lazy holders to confirm they are guarded by the same feature-flag checks as the startup method.

---

## Test

`repair/HappyPathFuzzTest` was extended to enable Accord repair and exercise the repaired code paths.
