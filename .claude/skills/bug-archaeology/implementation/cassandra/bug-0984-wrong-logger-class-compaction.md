# Bug 0984: Wrong Logger Class in CompactionAwareWriter

**Commit**: 3fc02dfcc7 · **Ticket**: n/a
**Subsystem**: Compaction
**Category**: Wrong constant / default value

---

## Root Cause
`CompactionAwareWriter` declared its `Logger` using `DefaultCompactionWriter.class` as the source class, causing all log output from the abstract base class to appear under the wrong class name. Because `CompactionAwareWriter` is the parent class, every subclass (not just `DefaultCompactionWriter`) inherited the mislabelled logger, making log correlation impossible.

### Before / After
```java
// BEFORE
protected static final Logger logger = LoggerFactory.getLogger(DefaultCompactionWriter.class);

// AFTER
protected static final Logger logger = LoggerFactory.getLogger(CompactionAwareWriter.class);
```

---

## How to Spot
When a `Logger` is declared with a class literal that does not match the class containing the declaration, grep for `LoggerFactory.getLogger(` and verify that the argument matches the enclosing class. This is especially easy to miss when copy-pasting a logger declaration from a subclass into a superclass.

---

## Test
No test added.
