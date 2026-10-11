# Bug 0792: Wrong Logger Class in AnticompactionTask

**Commit**: ac2a0a5f81 · **Ticket**: n/a
**Subsystem**: Repair / Logging
**Category**: Wrong constant / default value

---

## Root Cause
`AnticompactionTask` initialized its static `Logger` field using `RepairSession.class` as the source class argument to `LoggerFactory.getLogger(...)`. All log output from `AnticompactionTask` therefore appeared under the `RepairSession` logger name, making it impossible to filter or distinguish anticompaction log lines from repair session log lines via logback configuration.

### Before / After
```java
// BEFORE
private static Logger logger = LoggerFactory.getLogger(RepairSession.class);

// AFTER
private static Logger logger = LoggerFactory.getLogger(AnticompactionTask.class);
```

---

## How to Spot
Whenever a `Logger` field is declared as `static` and initialised with a hard-coded class literal, verify that the literal matches the enclosing class. Copy-paste errors are common here because the declaration is often cloned from a nearby class. A quick grep for `getLogger(` that cross-references its argument against the containing class name catches this pattern immediately.

---

## Test
No test added.
