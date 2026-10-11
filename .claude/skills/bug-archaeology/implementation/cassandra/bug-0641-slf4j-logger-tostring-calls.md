# Bug 0641: SLF4J Logger Calls Pass toString() Instead of Object

**Commit**: 5d4335e8f8 · **Ticket**: n/a
**Subsystem**: db/compaction, db/lifecycle, db/marshal, net/async
**Category**: Wrong constant / default value

---

## Root Cause

SLF4J's parameterized logging API accepts `Object` arguments and defers calling `toString()` until the log message is actually built (which is skipped entirely when the log level is disabled). Calling `.toString()` explicitly on the argument before passing it to the logger defeats this lazy evaluation: it always performs string conversion even when the level is disabled, and it also prevents the logger from printing exception stack traces when a `Throwable` is passed (SLF4J's special handling for the last argument being a `Throwable`). A separate issue in `InboundHandshakeHandler` used `+` string concatenation inside the log call, which forces eager evaluation and prevents SLF4J's guard optimization.

### Before / After

```java
// BEFORE
logger.warn("Using a non-default timestamp_resolution {} ...", timestampResolution.toString());
logger.debug("Unable to delete {} ...", baos.toString());
logger.error("Failed with [{}] ...", ce.toString());
logger.error("unhandled state: " + state);
logger.warn("Received stream using protocol version %d (my version %d).", version, MessagingService.current_version);
```

```java
// AFTER
logger.warn("Using a non-default timestamp_resolution {} ...", timestampResolution);
logger.debug("Unable to delete {} ...", baos);
logger.error("Failed with [{}] ...", ce);
logger.error("unhandled state: {}", state);
logger.warn("Received stream using protocol version {} (my version {}).", version, MessagingService.current_version);
```

---

## How to Spot

Any `logger.xxx(...)` call where an argument is followed by `.toString()`, or where the format string uses `%d`/`%s`-style printf placeholders instead of SLF4J's `{}`, or where string concatenation (`+`) is used directly inside the log call rather than as a separate argument — these are all signs that lazy evaluation is being bypassed. Code review tools and static analysis (e.g., SonarQube's `SSLF4J` rule set) can flag this pattern automatically.

---

## Test

No test added.
