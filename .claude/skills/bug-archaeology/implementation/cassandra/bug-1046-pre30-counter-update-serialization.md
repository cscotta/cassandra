# Bug 1046: Pre-3.0 Counter Update Cells Serialized with Wrong Format

**Commit**: df7c6581be · **Ticket**: CASSANDRA-10470
**Subsystem**: Storage engine / legacy serialization
**Category**: Wrong serialization / deserialization

---

## Root Cause
`LegacyLayout` did not distinguish between a local counter update cell (produced by `UpdateParameters.addCounter()`, containing only the local shard with a delta value) and a fully-resolved counter context cell. When serializing for pre-3.0 nodes, counter update cells were written using the standard counter format (`COUNTER_MASK` flag + dummy `Long.MIN_VALUE` timestamp-of-last-delete), instead of the counter-update format (`COUNTER_UPDATE_MASK` flag + timestamp + raw count). This meant counter increments sent to older nodes were not properly applied. The fix adds `isCounterUpdate()` detection — a cell is a counter update if it is a counter cell whose context `isLocal()` — and routes those cells through the correct serialization branch.

### Before / After
```java
// BEFORE — counter update and counter cell share the same branch
else if (cell.kind == LegacyLayout.LegacyCell.Kind.COUNTER) {
    out.writeByte(LegacyLayout.COUNTER_MASK);
    out.writeLong(Long.MIN_VALUE);  // always dummy value, even for updates
}

// AFTER — counter update cells get their own branch with correct encoding
else if (cell.isCounterUpdate()) {
    out.writeLong(cell.timestamp);
    long count = CounterContext.instance().getLocalCount(cell.value);
    ByteBufferUtil.writeWithLength(ByteBufferUtil.bytes(count), out);
    continue;
}
else if (cell.isCounter()) {
    out.writeLong(Long.MIN_VALUE);  // only for already-merged counters
}
```

---

## How to Spot
Serialization code that handles an enum `Kind` with a catch-all branch for a type that actually has multiple sub-variants (here: counter update vs. merged counter) is a signal to look closely. When adding a new internal cell kind to the 3.0 engine, every legacy serialization path must be audited to ensure each sub-variant maps to the correct wire format for older nodes. The presence of `CounterContext.isLocal()` as a distinguisher is the key runtime check — any serializer that does not call it is likely treating both variants identically.

---

## Test
No test added.
