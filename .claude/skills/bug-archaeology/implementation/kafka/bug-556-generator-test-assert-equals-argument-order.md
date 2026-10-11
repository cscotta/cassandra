**Commit**: a8b1fb9203 · **Ticket**: KAFKA-19777

**Subsystem**: generator/src/test/java/org/apache/kafka/message/MessageGeneratorTest.java

**Category**: Wrong constant/default value

**Root cause**
`assertEquals` calls in the message-generator tests had their arguments in the wrong order: the actual value was passed as the first argument and the expected value as the second, reversing the JUnit5 convention of `(expected, actual)`. This did not affect pass/fail outcomes but caused failure messages to say "expected: <actual>" which is misleading when debugging. The fix swaps the arguments to the correct `(expected, actual)` order.

**Before**
```java
// Before
assertEquals(MessageGenerator.UNSIGNED_SHORT_MAX, 0xFFFF);
assertEquals(MessageGenerator.UNSIGNED_INT_MAX, 0xFFFFFFFFL);
```

**After**
```java
// After
assertEquals(0xFFFF, MessageGenerator.UNSIGNED_SHORT_MAX);
assertEquals(0xFFFFFFFFL, MessageGenerator.UNSIGNED_INT_MAX);
```

**How to spot**
Search for `assertEquals` calls in test files where the first argument is a non-literal field reference and the second argument is a literal; JUnit5 convention is `assertEquals(expected-literal, actual-expression)`, so the reverse is a likely argument-order error.

**Test**
Introduce a deliberate regression (e.g. change the constant to a wrong value) and confirm the failure message now reads "expected: <correct> but was: <wrong>" rather than the reversed form.
