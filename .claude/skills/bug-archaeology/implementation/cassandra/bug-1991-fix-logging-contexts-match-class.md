# Bug 1991: Logger Contexts Reference Wrong Class in Repair Subsystem

**Commit**: 66f301457f · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/repair/Differencer.java, src/java/org/apache/cassandra/repair/RepairJob.java, src/java/org/apache/cassandra/repair/RepairSession.java, src/java/org/apache/cassandra/repair/Validator.java
**Category**: Wrong constant / default value

---

## Root Cause

Multiple classes in the `repair` package declared their `Logger` fields using `ActiveRepairService.class` as the logger category instead of their own class. This caused all log output from `Differencer`, `RepairJob`, `RepairSession`, and `Validator` to appear under the `ActiveRepairService` logger name, making it impossible to selectively adjust log levels for these classes and obscuring which class produced each log line.

### Before / After

```java
// BEFORE: wrong class reference in multiple repair classes
// In Differencer.java, RepairJob.java, RepairSession.java, Validator.java:
private static Logger logger = LoggerFactory.getLogger(ActiveRepairService.class);
```

```java
// AFTER: each class uses its own name
private static Logger logger = LoggerFactory.getLogger(Differencer.class);
// (similarly for RepairJob, RepairSession, Validator)
```

---

## How to Spot

When reviewing a newly extracted or refactored class, check that its `LoggerFactory.getLogger(...)` call references the containing class, not a superclass or the class from which it was copied. A `getLogger(SomeOtherClass.class)` in a file that does not define `SomeOtherClass` is almost always a copy-paste error.

---

## Test

No test added.
