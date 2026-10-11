**Commit**: 40db001588 · **Ticket**: KAFKA-18929
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
When producers send future timestamps, time retention based log segments
may get blocked from removal for an extended period of time. Log
cleaning should should warn in the logs when this scenario occurs. Reviewers: Viktor Somogyi-Vass <viktorsomogyi@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
  private val futureTimestampLogger = new LogFutureTimestampLogger(logIdent)
      if (startMs < segment.largestTimestamp()) {
        futureTimestampLogger.warn(s"$segment contains future timestamp(s), making it ineligible to be deleted")
      }
class LogFutureTimestampLogger(parentLogIdent: String) extends Logging {
  this.logIdent = parentLogIdent
}

```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18929: log a warning when time based segment delete is blocked by a future timestamp (#19137).

**Test**
No test added.
