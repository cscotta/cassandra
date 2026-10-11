# Bug 0660: InboundHandshakeHandler Logs Under Wrong Class Name

**Commit**: 4603600937 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/net/async/InboundHandshakeHandler.java
**Category**: Wrong constant / default value

---

## Root Cause

`InboundHandshakeHandler` obtained its `Logger` instance by passing `NettyFactory.class` to `LoggerFactory.getLogger()` instead of its own class. All log statements emitted by `InboundHandshakeHandler` appeared under the `NettyFactory` logger name, making it impossible to filter or route the handshake handler's log output independently, and causing confusion when diagnosing handshake failures because the log attribution pointed to the wrong class.

### Before / After

```java
// BEFORE
private static final Logger logger =
    LoggerFactory.getLogger(NettyFactory.class);
```

```java
// AFTER
private static final Logger logger =
    LoggerFactory.getLogger(InboundHandshakeHandler.class);
```

---

## How to Spot

Any time a class initializes a `Logger` with a class literal that does not match the enclosing class (copy-paste from a template or sibling class), log messages will be mis-attributed. Code reviewers should scan `LoggerFactory.getLogger(...)` calls and mechanically verify that the argument matches `<EnclosingClass>.class`. Static analysis tools (e.g., SLF4J's own `LoggerFactoryBound` rules or Checkstyle configurations) can catch this automatically.

---

## Test

No test added.
