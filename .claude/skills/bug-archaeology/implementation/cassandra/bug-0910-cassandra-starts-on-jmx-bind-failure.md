# Bug 0910: Cassandra Starts Successfully Despite JMX Bind Failure

**Commit**: 93c5bc616e · **Ticket**: CASSANDRA-11540
**Subsystem**: CassandraDaemon / JMX startup
**Category**: Logic error in condition

---

## Root Cause
`CassandraDaemon.maybeInitJmx()` caught `IOException` from binding the local JMX server socket and logged it as an error, but allowed startup to continue. This meant operators believed Cassandra was running normally while JMX was silently unavailable — blocking all `nodetool` commands and monitoring agents that rely on JMX. The fix changes the catch block to call `exitOrFail(1, ...)` so the process terminates with a non-zero exit code, making the failure visible and forcing operators to fix the configuration before Cassandra can start. The method is also changed from `static` to an instance method so `exitOrFail` (an instance method) can be called directly.

### Before / After
```java
// BEFORE — logs and continues
catch (IOException e) {
    logger.error("Error starting local jmx server: ", e);
}

// AFTER — terminates the process
catch (IOException e) {
    exitOrFail(1, e.getMessage(), e.getCause());
}
```

---

## How to Spot
In daemon startup code, catch blocks that only log errors for critical infrastructure (JMX, storage directories, commit log) are dangerous: the process appears healthy from the outside while a core capability is missing. During review, ask whether each caught exception in a startup method represents a recoverable partial-feature loss or a hard requirement. If other components of the system (monitoring, management, repair) depend on the failed service, the right response is almost always to fail fast rather than continue silently.

---

## Test
No test added.
