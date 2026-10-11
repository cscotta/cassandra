# Bug 1266: Fix Remote Debugging Configuration

**Commit**: 9b77cdea76 · **Ticket**: CASSANDRA-9297
**Subsystem**: conf/cassandra-env.sh
**Category**: Wrong constant / default value

---

## Root Cause

The commented-out example JVM remote debugging flags in `cassandra-env.sh` used the legacy JDWP activation syntax (`-Xdebug -Xnoagent -Xrunjdwp:...`) which was deprecated and removed in modern JDKs. Operators who uncommented these lines to enable remote debugging would find the JVM refusing to start or ignoring the flags on current JVMs. The correct modern form is the single `-agentlib:jdwp=...` agent argument.

### Before / After

```bash
# BEFORE
# JVM_OPTS="$JVM_OPTS -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1414"
```

```bash
# AFTER
# JVM_OPTS="$JVM_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=1414"
```

---

## How to Spot

Configuration file examples using deprecated or removed JVM flags will fail silently or loudly for operators on modern JDKs. Periodically audit commented example JVM options against the JVM version matrix the project supports, especially during JDK major-version bumps.

---

## Test

No test added.
