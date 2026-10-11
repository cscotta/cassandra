# Bug 2032: IOException Incorrectly Declared on StorageProxy.cas() Throws Clause

**Commit**: 56237c9164 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/service/StorageProxy.java
**Category**: Logic error in condition

---

## Root Cause

The `StorageProxy.cas()` method declared `IOException` in its throws clause, but the method no longer threw that checked exception after surrounding refactoring. The spurious declaration caused compilation errors in callers that did not (and could not) handle `IOException`, breaking the build.

### Before / After

```java
// BEFORE
public static boolean cas(String table, String cfName, ByteBuffer key, ColumnFamily expected, ColumnFamily updates)
throws UnavailableException, IOException, IsBootstrappingException, ReadTimeoutException, WriteTimeoutException, InvalidRequestException
```

```java
// AFTER
public static boolean cas(String table, String cfName, ByteBuffer key, ColumnFamily expected, ColumnFamily updates)
throws UnavailableException, IsBootstrappingException, ReadTimeoutException, WriteTimeoutException, InvalidRequestException
```

---

## How to Spot

When a method's throws clause lists a checked exception that the method body no longer propagates, the compiler will flag callers that declare the same exception but the method signature itself will still compile. Review throws clauses after refactoring to ensure they reflect only what the method actually throws.

---

## Test

No test added.
