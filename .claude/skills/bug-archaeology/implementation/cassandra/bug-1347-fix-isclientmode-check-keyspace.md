# Bug 1347: Fix isClientMode check in Keyspace

**Commit**: 2dd6944f1c · **Ticket**: CASSANDRA-8687
**Subsystem**: src/java/org/apache/cassandra/db/Keyspace.java
**Category**: Logic error in condition

---

## Root Cause

The static initializer in `Keyspace` called `StorageService.instance.isClientMode()` to determine whether to create data directories. `StorageService` is a singleton that initializes lazily, and accessing it during static initialization of `Keyspace` in a client-mode JVM (e.g., from `CQLSSTableWriter`) could trigger premature initialization of `StorageService` or read an incorrect mode flag, causing directory creation to proceed when it should not. A separate `Config.isClientMode()` flag is set earlier and more reliably during client-mode startup. The fix ORs both checks together so that either signal is sufficient to suppress directory creation.

### Before / After

```java
// BEFORE
if (!StorageService.instance.isClientMode())
    DatabaseDescriptor.createAllDirectories();
```

```java
// AFTER
if (!(Config.isClientMode() || StorageService.instance.isClientMode()))
    DatabaseDescriptor.createAllDirectories();
```

---

## How to Spot

When a static initializer or early-lifecycle code queries a lazily initialized singleton to decide whether to perform side-effectful operations, check whether a lighter-weight, earlier-set flag exists for the same concept. Accessing a heavyweight singleton in a static block can cause initialization ordering issues; prefer a plain static boolean that is set before any class loading begins.

---

## Test

No test added.
