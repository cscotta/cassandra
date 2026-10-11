# Bug 0828: NPE in SSTableLoader for partial directory path

**Commit**: 893fd21b52 · **Ticket**: CASSANDRA-12609
**Subsystem**: tools / BulkLoader (sstableloader)
**Category**: Missing null / bounds check

---

## Root Cause
`BulkLoader` passed `options.directory` directly to `SSTableLoader`. When the user supplies a relative or partial path (e.g. `data/ks/table`) the `File` object's `getParent()` or parent-traversal logic inside `SSTableLoader` returns `null` because the path has no explicit parent component, triggering a NullPointerException. Calling `getAbsoluteFile()` first resolves the path against the JVM's working directory, ensuring that every path component is present before `SSTableLoader` attempts to walk up the directory tree.

### Before / After
```java
// BEFORE — relative path, getParent() may return null
SSTableLoader loader = new SSTableLoader(
    options.directory, ...);

// AFTER — absolute path guaranteed
SSTableLoader loader = new SSTableLoader(
    options.directory.getAbsoluteFile(), ...);
```

---

## How to Spot
Any code that navigates upward through a `java.io.File` hierarchy (`.getParentFile()`, `.getParent()`) without first canonicalising or absolutising the path will NPE on relative inputs. At tool-entry-point boundaries, always resolve user-supplied paths to absolute form before passing them deeper into the system.

---

## Test
No new test added.
