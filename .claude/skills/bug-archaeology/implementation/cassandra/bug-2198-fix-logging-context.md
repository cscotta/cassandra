# Bug 2198: ThriftServer Logger Attributed Entries to Wrong Class

**Commit**: ab2614ca15 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/thrift/ThriftServer.java
**Category**: Wrong constant / default value

---

## Root Cause

`ThriftServer` obtained its `Logger` instance using `CassandraDaemon.class` as the category instead of `ThriftServer.class`. All log output from `ThriftServer` therefore appeared under the `CassandraDaemon` logger name, making it impossible to selectively adjust the log level for `ThriftServer` or to correctly attribute log messages to their source class during troubleshooting.

### Before / After

```java
// BEFORE – wrong class used for logger category
private static Logger logger = LoggerFactory.getLogger(CassandraDaemon.class);
```

```java
// AFTER – logger correctly attributed to ThriftServer
private static Logger logger = LoggerFactory.getLogger(ThriftServer.class);
```

---

## How to Spot

During code review, verify that every `LoggerFactory.getLogger(...)` call passes the enclosing class, not an unrelated class. A quick static check or IDE inspection can flag any logger whose category does not match the class it is declared in.

---

## Test

No test added.
