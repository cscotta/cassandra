# Bug 1115: sstablerepairedset Tool Crashes Because Client Mode Not Set

**Commit**: 707b2da11c · **Ticket**: CASSANDRA-10132
**Subsystem**: src/java/org/apache/cassandra/tools/SSTableRepairedAtSetter.java
**Category**: Missing null / bounds check

---

## Root Cause

`SSTableRepairedAtSetter` (the `sstablerepairedset` offline tool) reads SSTable files using `RandomAccessReader`, which internally allocates buffers from `BufferPool`. `BufferPool` initialization requires `DatabaseDescriptor` to be configured. Standalone tools must call `Config.setClientMode(true)` before any I/O so that `DatabaseDescriptor` initialises in a lightweight mode without trying to bind ports or start services. This call was missing, causing the tool to fail at startup when `BufferPool` attempted to access the uninitialised descriptor.

### Before / After

```java
// BEFORE
public static void main(final String[] args) throws IOException
{
    PrintStream out = System.out;
    if (args.length == 0)
    { ...
```

```java
// AFTER
public static void main(final String[] args) throws IOException
{
    // Necessary since BufferPool used in RandomAccessReader needs to access DatabaseDescriptor
    Config.setClientMode(true);

    PrintStream out = System.out;
    if (args.length == 0)
    { ...
```

---

## How to Spot

Any standalone offline Cassandra tool that performs direct SSTable I/O must call `Config.setClientMode(true)` before touching any I/O class that transitively references `DatabaseDescriptor`. When adding or reviewing such tools, check the `main()` method for this guard as the very first action. The absence of this call is easy to miss because the failure surfaces as a seemingly unrelated initialisation error deep inside `BufferPool` rather than pointing at the missing client-mode flag.

---

## Test

No test added.
