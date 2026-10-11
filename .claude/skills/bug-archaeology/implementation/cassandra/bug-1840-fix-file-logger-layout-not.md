# Bug 1840: Fix File Logger Layout Not Dumping Stacktrace Over Lines

**Commit**: 1141b82d68 · **Ticket**: n/a
**Subsystem**: conf/logback.xml (logging configuration)
**Category**: Wrong constant / default value

---

## Root Cause

The logback rolling-file appender used `%caller{1}` in its pattern, which forces logback to capture and format a full caller stack frame as a multi-line annotation. This caused stack traces and caller information to spill across multiple lines in the log file rather than keeping each log event on a single line. Replacing `%caller{1}` with `%F:%L` (filename and line number) produces the same essential location information as a compact inline token.

### Before / After

```java
// BEFORE (conf/logback.xml)
<pattern>%-5level [%thread] %date{ISO8601} %caller{1} - %msg%n</pattern>
```

```java
// AFTER (conf/logback.xml)
<pattern>%-5level [%thread] %date{ISO8601} %F:%L - %msg%n</pattern>
```

---

## How to Spot

When reviewing logback/log4j patterns, `%caller{N}` always emits multi-line output because it includes a formatted stack frame block; prefer `%F:%L` or `%C:%M` for single-line source location.

---

## Test

No test added.
