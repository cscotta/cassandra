# Bug 0607: Spelling Errors in Log Messages in NativeTransportService

**Commit**: e04462537c · **Ticket**: n/a
**Subsystem**: `src/java/org/apache/cassandra/service/NativeTransportService.java`
**Category**: Wrong constant / default value

---

## Root Cause

Two typographical errors existed in `NativeTransportService`: the Javadoc comment described "epoll bassed" and the log warning message said "epoll not availble". These were non-functional spelling mistakes that reduced the readability of log output and documentation. The fix corrects both strings to "epoll based" and "epoll not available".

### Before / After

```java
// BEFORE
/** @return intend to use epoll bassed event looping */
...
logger.warn("epoll not availble {}", Epoll.unavailabilityCause());
```

```java
// AFTER
/** @return intend to use epoll based event looping */
...
logger.warn("epoll not available {}", Epoll.unavailabilityCause());
```

---

## How to Spot

Spelling errors in log messages are typically caught during code review or by automated spellchecking tools integrated into the CI pipeline. The log string "not availble" would not match patterns that operators might grep for in their monitoring tooling.

---

## Test

No test added.
