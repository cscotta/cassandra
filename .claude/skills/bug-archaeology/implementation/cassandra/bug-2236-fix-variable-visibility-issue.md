# Bug 2236: ResultMessage.SetKeyspace.keyspace Field Inaccessible to External Callers

**Commit**: 16f3d33f33 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/transport/messages/ResultMessage.java
**Category**: Missing override of default method

---

## Root Cause

`ResultMessage.SetKeyspace` stores the name of the keyspace that a `USE` statement switched to. The `keyspace` field was declared `private`, but code outside the class (specifically in the native transport layer handling `USE` responses) needed to read it directly. Because no getter was provided, any external access compiled only if it reached a subclass or inner scope with access — or it silently failed to compile at all for external packages. The fix simply widens the visibility to `public`.

### Before / After

```java
// BEFORE
private final String keyspace;
```

```java
// AFTER
public final String keyspace;
```

---

## How to Spot

When a data-holding inner class of a message type stores a field `private` with no accessor, callers that need to read the value are forced to use reflection or will fail to compile. During review, any inner class that acts as a plain data carrier (DTO) should expose its fields at the visibility level appropriate for its consumers.

---

## Test

No test added.
