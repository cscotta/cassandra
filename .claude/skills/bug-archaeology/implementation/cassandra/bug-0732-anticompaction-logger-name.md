# Bug 0732: Wrong logger class in AnticompactionTask

**Commit**: a69f688555 · **Ticket**: CASSANDRA-13343
**Subsystem**: Repair / Logging
**Category**: Wrong constant / default value

---

## Root Cause
`AnticompactionTask` initialised its `Logger` with `RepairSession.class` instead of `AnticompactionTask.class`. As a result, all log output from anticompaction tasks appeared under the `RepairSession` logger name, making it impossible to selectively enable or disable anticompaction logging independently, and causing log messages to be misleadingly attributed to the repair session subsystem during debugging.

### Before / After
```java
// BEFORE
private static Logger logger = LoggerFactory.getLogger(RepairSession.class);

// AFTER
private static Logger logger = LoggerFactory.getLogger(AnticompactionTask.class);
```

---

## How to Spot
During code review, verify that every `LoggerFactory.getLogger(X.class)` call is in the class named `X`. Copy-paste errors that leave the wrong class reference are common and are invisible at runtime except through misdirected log output.

---

## Test
No test added.
