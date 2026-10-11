# Bug 0224: Unnecessary Deserialization of Terminal Arguments in CQL Function Execution

**Commit**: 4f3cb5de37 · **Ticket**: CASSANDRA-18566
**Subsystem**: cql3/functions
**Category**: Wrong serialization / deserialization

---

## Root Cause

When executing CQL scalar functions and user-defined functions, argument `ByteBuffer` values were passed as a raw `List<ByteBuffer>` and each implementation immediately deserialized them into Java objects via `AbstractType.compose()`. For constant (terminal) arguments that do not change across rows, this round-trip deserialization happened redundantly on every invocation. The fix introduces an `Arguments` interface backed by `FunctionArguments`, which lazily deserializes each argument at most once per invocation and caches the result, and changes `ScalarFunction.execute()` to accept `Arguments` instead of `List<ByteBuffer>`. This eliminates the repeated object creation overhead for constant arguments.

### Before / After

```java
// BEFORE
public ByteBuffer execute(ProtocolVersion protocolVersion, List<ByteBuffer> parameters);
// caller:
List<ByteBuffer> buffers = new ArrayList<>(terms.size());
for (Term t : terms) buffers.add(t.bindAndGet(options));
return executeInternal(options.getProtocolVersion(), fun, buffers);
```

```java
// AFTER
public ByteBuffer execute(Arguments arguments);
// caller:
Arguments arguments = fun.newArguments(options.getProtocolVersion());
for (int i = 0; i < terms.size(); i++) arguments.set(i, terms.get(i).bindAndGet(options));
return executeInternal(fun, arguments);
```

---

## How to Spot

Function APIs that accept raw `List<ByteBuffer>` and deserialize in the body on every call are candidates for this pattern. When argument types are known at compile time and arguments may be constant across rows, caching deserialized values in a typed container avoids repeated work. Review hot paths in selection and aggregate evaluation for repeated `AbstractType.compose()` calls on identical buffers.

---

## Test

A microbenchmark (`FunctionWithTerminalArgsBench`) was added to measure the performance improvement; no correctness-only test was added for the bug itself.
