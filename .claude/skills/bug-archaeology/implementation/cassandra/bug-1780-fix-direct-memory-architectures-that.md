# Bug 1780: Fix direct Memory on architectures that do not support unaligned access

**Commit**: 039e9b9a18 · **Ticket**: CASSANDRA-6628
**Subsystem**: io/util (Memory), config (DatabaseDescriptor), utils (FastByteComparisons)
**Category**: Wrong constant / default value

---

## Root Cause

`Memory.setLong/getLong/setInt/getInt` used `sun.misc.Unsafe` multi-byte put/get operations directly on arbitrary off-heap addresses. On architectures that require aligned memory access (e.g., SPARC, ARM), unaligned multi-byte reads/writes via Unsafe cause bus errors or silently corrupt data. The code assumed all platforms supporting a 64-bit OS also support unaligned access, which is false. Additionally, `DatabaseDescriptor` and `CassandraDaemon` used a naive `os.arch.contains("64")` check that misidentifies SPARC v9 (`sparcv9`) and relies on the JVM property `sun.arch.data.model` not being present.

### Before / After

```java
// BEFORE (Memory.java)
public void setLong(long offset, long l) {
    checkPosition(offset);
    unsafe.putLong(peer + offset, l);
}
```

```java
// AFTER (Memory.java)
public void setLong(long offset, long l) {
    checkPosition(offset);
    if (unaligned) {
        unsafe.putLong(peer + offset, l);
    } else {
        putLongByByte(peer + offset, l);
    }
}
```

---

## How to Spot

Every use of `Unsafe` multi-byte operations on off-heap memory must be guarded by an explicit unaligned-access capability check. Detecting 64-bit by string-matching `os.arch` is fragile; prefer `sun.arch.data.model` with a known-unaligned architecture allowlist as a fallback.

---

## Test

No test added.
