# Bug 0264: JMX Missing Authorization Proxy Allows Remote Code Execution

**Commit**: b2660bcf78 · **Ticket**: CASSANDRA-17921
**Subsystem**: src/java/org/apache/cassandra/auth/jmx
**Category**: Missing null / bounds check

---

## Root Cause

Cassandra's JMX endpoint did not install an `MBeanServerForwarder` to intercept and block dangerous JMX operations before delegating to the underlying `MBeanServer`. This left several well-known "beanshooter" attack vectors open: invoking `compilerDirectivesAdd` or `jvmtiAgentLoad` on `DiagnosticCommand` (which can read arbitrary files or load native libraries), and calling `addURL`/`getMBeansFromURL` on any `MLet` MBean (which can load arbitrary JARs from attacker-controlled URLs). The fix introduces `AuthorizationProxy`, a new `InvocationHandler` that sits in front of the `MBeanServer` and denies these specific dangerous operations unconditionally.

### Before / After

```java
// BEFORE – no forwarder installed; all MBeanServer operations reach the platform directly
jmxServer = new RMIConnectorServer(url, env, server, ManagementFactory.getPlatformMBeanServer());
jmxServer.start();
```

```java
// AFTER – optional AuthorizationProxy forwarder blocks dangerous ops
jmxServer = new RMIConnectorServer(url, env, server, ManagementFactory.getPlatformMBeanServer());
if (authzProxy != null)
    jmxServer.setMBeanServerForwarder(authzProxy);
jmxServer.start();
```

---

## How to Spot

Any `MBeanServer` exposed over a network interface should have a deny-list layer in front of it before the `platform MBeanServer` is consulted. During review, look for `RMIConnectorServer` construction without a subsequent `setMBeanServerForwarder` call; the absence of that call means every JMX operation—including ones known to enable arbitrary file reads and library loading—goes directly to the JVM.

---

## Test

No dedicated unit test added; the fix is verified by manual beanshooter testing as described in the ticket.
