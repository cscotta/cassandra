**Commit**: e04fd9d8bd · **Ticket**: KAFKA-19683

**Subsystem**: streams/src/test/java/org/apache/kafka/streams/processor/internals/TaskManagerTest.java

**Category**: State not cleaned up

**Root cause**
Multiple `TaskManagerTest` methods used the deprecated `new ConsumerGroupMetadata("appId")` constructor, which was scheduled for removal and required `@SuppressWarnings("removal")` annotations. The constructor also constructed a partially populated object unsuitable for full mock-based verification. Replacing it with `mock(ConsumerGroupMetadata.class)` removes the suppression, allows `when(...).thenReturn(...)` stubs to control exactly what methods return, and keeps the test aligned with the always-enabled `StateUpdater` path.

**Before**
```java
// Before
@SuppressWarnings("removal")
final ConsumerGroupMetadata groupMetadata =
    new ConsumerGroupMetadata("appId");
```

**After**
```java
// After
final ConsumerGroupMetadata groupMetadata =
    mock(ConsumerGroupMetadata.class);
```

**How to spot**
Search `TaskManagerTest` for `new ConsumerGroupMetadata(` — any usage of the deprecated single-string constructor is a likely `@SuppressWarnings("removal")` candidate and should be replaced with a mock.

**Test**
Run `TaskManagerTest` and confirm all previously suppressed compilation warnings are gone and all affected tests still pass.
