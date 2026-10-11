# Bug 0205: NPE When Using udfContext in UDF After Node Restart

**Commit**: 998f84a2cb · **Ticket**: CASSANDRA-18739
**Subsystem**: cql3/functions (UDFContextImpl, UDFunction)
**Category**: Missing null / bounds check

---

## Root Cause

`UDFunction`'s constructor eagerly resolved `Schema.instance.getKSMetaData(name.keyspace)` at the moment the function object was created and stored the result directly in `UDFContextImpl`. During a node restart, UDFs are loaded from system tables and their constructors run before the keyspace schema is fully registered in the in-memory `Schema`; `getKSMetaData` returned `null` and any subsequent call to `udfContext.newUDTValue()` or `newTupleValue()` threw a NullPointerException dereferencing the null `KeyspaceMetadata`. The fix defers keyspace-metadata lookup to first use via a memoized `Supplier`, ensuring the schema is available by the time the UDF is actually called.

### Before / After

```java
// BEFORE – eager lookup at construction time; null if schema not yet loaded
KeyspaceMetadata keyspaceMetadata = Schema.instance.getKSMetaData(name.keyspace);
this.udfContext = new UDFContextImpl(argNames, argCodecs, returnCodec, keyspaceMetadata);
```

```java
// AFTER – lazy lookup memoized on first use
this.udfContext = new UDFContextImpl(argNames, argCodecs, returnCodec, name.keyspace);
// inside UDFContextImpl:
this.metadata = Suppliers.memoize(() -> Schema.instance.getKSMetaData(keyspace));
```

---

## How to Spot

Any object whose constructor calls a registry or service (e.g., `Schema.instance`, `Keyspace.open()`) to hydrate state is fragile when construction can occur before the service is fully initialized—especially during replay or restart. Prefer lazy initialization with a memoized supplier for schema-dependent fields, and add a comment explaining why the lookup is deferred.

---

## Test

Distributed test verifies that calling a UDF that invokes `udfContext.newUDTValue()` succeeds both before and after a full node shutdown and restart.
