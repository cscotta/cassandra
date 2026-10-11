# Bug 0861: Logger initialized with wrong class in DateTieredCompactionStrategyOptions

**Commit**: be24013bd8 · **Ticket**: n/a
**Subsystem**: Compaction / DateTieredCompactionStrategy
**Category**: Wrong constant / default value

---

## Root Cause
`DateTieredCompactionStrategyOptions` declared its `Logger` using the outer strategy class `DateTieredCompactionStrategy.class` instead of the options class itself. As a result, all log messages emitted from `DateTieredCompactionStrategyOptions` appeared under the wrong logger name, making it impossible to selectively enable or filter logging for the options-parsing code path and causing misleading attribution in log output.

### Before / After
```java
// BEFORE
private static final Logger logger = LoggerFactory.getLogger(DateTieredCompactionStrategy.class);

// AFTER
private static final Logger logger = LoggerFactory.getLogger(DateTieredCompactionStrategyOptions.class);
```

---

## How to Spot
This is a classic copy-paste mistake when creating a companion options or configuration class next to a primary strategy class. In any class that is split from a parent (e.g., `FooOptions` split from `Foo`), verify that the `LoggerFactory.getLogger(...)` argument matches the declaring class, not the class it was copied from. Static analysis tools or a simple convention of using `MethodHandles.lookup().lookupClass()` would prevent this category of error entirely.

---

## Test
No test added. The fix is a one-line correction of the logger class reference.
