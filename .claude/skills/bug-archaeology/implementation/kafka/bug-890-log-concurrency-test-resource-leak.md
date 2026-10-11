**Commit**: 007fe6e92a · **Ticket**: KAFKA-19466

**Subsystem**: Core / LogConcurrencyTest

**Category**: Resource leak

**Root cause**
`LogConcurrencyTest` created `UnifiedLog` instances via `createLog()` but never closed them, leaving file handles and background threads open after each test method. Over a test run this accumulated into resource exhaustion. The test was rewritten in Java with a proper `@AfterEach` teardown that calls `log.close()` for every created log, and moved to the storage module where it can access the necessary APIs directly.

**Before/After**
```java
// Before (Scala): log created, never closed
val log = createLog(...)
// ... test body ...
// no close()

// After (Java): log closed in @AfterEach
private UnifiedLog log;

@AfterEach
void tearDown() throws Exception {
    if (log != null) log.close();
}
```

**How to spot**
Test classes that create file-backed objects (`UnifiedLog`, `FileRecords`, `AbstractIndex`) without corresponding `@AfterEach` or try-with-resources cleanup will leak file descriptors. Search for `createLog(` calls not paired with `log.close()` in the same or a teardown method.

**Test**
`LogConcurrencyTest` (Java rewrite) verifies concurrent read/write operations on a `UnifiedLog` and guarantees cleanup via `@AfterEach`.
