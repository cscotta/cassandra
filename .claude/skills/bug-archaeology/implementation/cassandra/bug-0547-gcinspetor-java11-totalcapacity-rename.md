# Bug 0547: GCInspector Fails to Read Off-Heap Memory on Java 11

**Commit**: 2ed2b87b63 · **Ticket**: CASSANDRA-14757
**Subsystem**: `src/java/org/apache/cassandra/service/GCInspector.java`
**Category**: Wrong constant / default value

---

## Root Cause

`GCInspector` reported off-heap (direct) memory usage by accessing the private field `java.nio.Bits.totalCapacity` via reflection. In Java 11 that field was renamed to `TOTAL_CAPACITY` (SCREAMING_SNAKE_CASE) to match Java naming conventions for constants. Because the code hard-coded only the old name, startup on Java 11 threw `NoSuchFieldException` and silently disabled off-heap tracking; the `NoSuchFieldException` was caught by a broad outer catch and the field reference was left null.

### Before / After

```java
// BEFORE
Field f = bitsClass.getDeclaredField("totalCapacity");
f.setAccessible(true);
```

```java
// AFTER
Field f;
try {
    f = bitsClass.getDeclaredField("totalCapacity");
}
catch (NoSuchFieldException ex) {
    // in Java11 it changed name to "TOTAL_CAPACITY"
    f = bitsClass.getDeclaredField("TOTAL_CAPACITY");
}
f.setAccessible(true);
```

---

## How to Spot

Every reflective field or method access by hard-coded string name is a latent compatibility hazard. When reviewing such code, check whether the JDK version matrix tested in CI matches the full range of supported runtime versions, and whether the code handles `NoSuchFieldException` / `NoSuchMethodException` by trying alternative names rather than silently degrading. Private JDK internals are especially risky because their names can change in any release.

---

## Test

No test added; the fix is a defensive fallback to the Java 11 field name.
