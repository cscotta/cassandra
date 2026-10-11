# Bug 0616: Wrong Class Passed to LoggerFactory in ViewBuilder and RangeDenormalizer

**Commit**: 21be9d2f50 · **Ticket**: n/a
**Subsystem**: `db/view/ViewBuilder.java`, `repair/asymmetric/RangeDenormalizer.java`
**Category**: Wrong constant / default value

---

## Root Cause

Two classes declared their `Logger` field using a sibling or unrelated class as the argument to `LoggerFactory.getLogger()` rather than their own class. This is a copy-paste error: `ViewBuilder` referenced `ViewBuilderTask.class`, and `RangeDenormalizer` referenced `IncomingRepairStreamTracker.class`. As a result, log output from those classes appeared under the wrong logger name, making diagnostics misleading and potentially causing misconfigured log-level filtering for those components.

### Before / After

```java
// BEFORE (ViewBuilder.java)
private static final Logger logger = LoggerFactory.getLogger(ViewBuilderTask.class);

// BEFORE (RangeDenormalizer.java)
private static final Logger logger = LoggerFactory.getLogger(IncomingRepairStreamTracker.class);
```

```java
// AFTER (ViewBuilder.java)
private static final Logger logger = LoggerFactory.getLogger(ViewBuilder.class);

// AFTER (RangeDenormalizer.java)
private static final Logger logger = LoggerFactory.getLogger(RangeDenormalizer.class);
```

---

## How to Spot

Whenever a class declares a `static final Logger` field, verify that the class literal passed to `getLogger()` matches the enclosing class name exactly. Static analysis tools (e.g., PMD's `LoggerIsNotStaticFinal` or IntelliJ's inspections) flag mismatches; a reviewer should grep for `getLogger(` and spot-check that the class argument is `<ClassName>.class`, not any other class from the same package.

---

## Test

No test added.
