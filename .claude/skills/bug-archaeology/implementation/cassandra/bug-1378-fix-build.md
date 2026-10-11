# Bug 1378: CQLSSTableWriter Static Initializer Unconditionally Forces Client Mode

**Commit**: 382834d4e4 · **Ticket**: CASSANDRA-8281
**Subsystem**: src/java/org/apache/cassandra/io/sstable/CQLSSTableWriter.java
**Category**: Logic error in condition

---

## Root Cause

A static initializer block was added to `CQLSSTableWriter` to call `Config.setClientMode(true)`, intending to ensure the class always operates in client mode. However, a Java static initializer fires the first time the class is loaded by the JVM, regardless of whether any instance is ever created or used. This meant that any code path that triggered class loading of `CQLSSTableWriter` — including tests that merely referenced the type — would silently flip the entire JVM into client mode, breaking unrelated server-mode components and causing build failures.

### Before / After

```java
// BEFORE: static block forces client mode on class load
static
{
    // The CQLSSTableWriter must always be used in client mode.
    Config.setClientMode(true);
}
```

```java
// AFTER: static block removed; client mode must be set by the caller
// (no static initializer)
```

---

## How to Spot

Static initializers that set global or singleton state are a reliability hazard: the side effect fires at class-load time, which can happen long before any instance is constructed and from surprising call sites (reflection, test framework class scanning, etc.). A reviewer should flag any `static { }` block that mutates shared JVM-wide state (system properties, singletons, static fields of other classes) and ask whether a constructor, factory method, or explicit setup call would be a safer boundary.

---

## Test

No test added; the fix was verified by the build itself recovering from the broken state.
