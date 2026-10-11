**Commit**: 718202dbf4 · **Ticket**: KAFKA-15853
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
    public static void swallow(final Logger log, final Level level, final String what, final SwallowAction code) {
        swallow(log, level, what, code, null);
    public static void swallow(final Logger log, final Level level, final String what, final SwallowAction code,
                        log.info(what, t);
                        log.debug(what, t);
                        log.error(what, t);
                        log.trace(what, t);
                        log.warn(what, t);
import kafka.utils.CoreUtils.{inReadLock, inWriteLock}

// After
import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;

    public static void swallow(final SwallowAction code) {
        swallow(log, Level.WARN, "Exception while execution the action", code, null);
    }

    public static void swallow(final Logger log, final SwallowAction code) {
        swallow(log, Level.WARN, "Exception while execution the action", code, null);
    }

```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15853: delete coreutils.scala and migrate logic to java (#21289).

**Test**
Test modified in TestRaftServer.scala covering the corrected behaviour.
