# Bug 0950: UDF Sandbox Allows Logging via Driver DataType Static Initializer

**Commit**: a76a8efcc7 · **Ticket**: CASSANDRA-11033
**Subsystem**: CQL / User-Defined Functions / Sandbox
**Category**: State not cleaned up

---

## Root Cause
Java UDFs were compiled and executed with a security-manager sandbox that prohibited network and file I/O to prevent user code from accessing sensitive resources. The UDF infrastructure stored `DataType` instances (from the Cassandra Java driver) as fields and passed them to helper methods at execution time. `DataType` static initialization and certain `DataType` method calls triggered loading of the driver's codec registry, which internally initialised logback appenders — a side effect that happened inside the sandbox's restricted class-loading context. Because logback tries to open files and sockets during initialisation, this caused `AccessControlException`s inside the UDF rather than in the surrounding host code.

The fix replaces all `DataType` fields and parameters in `UDFunction`, `JavaUDF`, and `UDHelper` with `TypeCodec` instances. Codecs are resolved once outside the sandbox (at UDF creation time) via `UDHelper.codecFor(dataType)`, and only the already-resolved codec — which has no further static I/O initialisation — is passed into the sandboxed execution path. The pre-warm call in `setupUdfClassLoader` is updated to call `TypeCodec.inet()` and `TypeCodec.ascii()` directly, ensuring the logback chain is loaded before the sandbox is active.

### Before / After
```java
// BEFORE — DataType passed into sandbox; triggers codec/logback init inside sandbox
protected final DataType[] argDataTypes;
protected final DataType returnDataType;
UDHelper.codecFor(DataType.inet()).format(InetAddress.getLoopbackAddress()); // pre-warm

// AFTER — TypeCodec resolved outside sandbox; no I/O init inside sandboxed path
protected final TypeCodec<Object>[] argCodecs;
protected final TypeCodec<Object> returnCodec;
TypeCodec.inet().format(InetAddress.getLoopbackAddress()); // pre-warm
```

---

## How to Spot
In any sandboxed execution environment, trace every object that flows through the sandbox boundary. If a parameter type has static initializers or lazy class loading that triggers I/O (logback, JDBC drivers, codec registries), it must be fully initialised in the host context before being passed into the sandbox. A pre-warm helper that calls `getClass()` or `format("")` on each type outside the sandbox is the standard mitigation; verify it covers all paths.

---

## Test
`AggregationTest` was extended with additional UDF/UDA tests. Test-resource files (`udfverify/*.java`) were updated to use the new `TypeCodec`-based constructor signature.
