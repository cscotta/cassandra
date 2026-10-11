**Commit**: bf914b6982 · **Ticket**: KAFKA-16330
**Subsystem**: streams/processor/TaskId
**Category**: Missing override of default method

**Root cause**
`TaskId` exposed `topicGroupId` and `partition` as deprecated `public final` fields, and also had deprecated `writeTo` / `readFrom` methods delegating to internal serialization utilities. Keeping these members public prevented encapsulation of the internal layout and kept callers coupled to raw integer fields. The fix makes the two fields `private final` (breaking any direct field access) and removes the four deprecated I/O methods entirely, directing callers to the `ConsumerProtocolUtils` helpers directly.

**Before/After**
```java
// Before
@Deprecated public final int topicGroupId;
@Deprecated public final int partition;
@Deprecated public void writeTo(DataOutputStream out, int version) throws IOException { ... }
@Deprecated public static TaskId readFrom(DataInputStream in, int version) throws IOException { ... }

// After
private final int topicGroupId;
private final int partition;
// writeTo / readFrom methods removed
```

**How to spot**
Public fields that carry `@Deprecated` are an API liability: they cannot be made private without a source-incompatible change. Schedule their removal for the next major version and add a `@deprecated` JavaDoc comment at deprecation time so users have a migration window.

**Test**
All callers that used the deprecated fields or methods updated to use accessor methods or `ConsumerProtocolUtils` directly.
