**Commit**: 31330e2e68 · **Ticket**: KAFKA-19777

**Subsystem**: generator/src/test/java/org/apache/kafka/message/CodeBufferTest.java

**Category**: Wrong constant/default value

**Root cause**
`assertEquals` calls in the `generator` package tests (`CodeBufferTest`, `StructRegistryTest`, `VersionConditionalTest`) had their arguments reversed: the actual expression was in the first position and the expected literal in the second. JUnit5's `assertEquals(expected, actual)` convention means failures print "expected: <actual> but was: <expected>", which is backwards and confuses debugging. The fix swaps arguments to the correct `(expected, actual)` order throughout these three test files.

**Before**
```java
// Before
assertEquals(codeBuffer.toString(), expectedString);
```

**After**
```java
// After
assertEquals(expectedString, codeBuffer.toString());
```

**How to spot**
In the `generator` test package, look for `assertEquals(variable, literal)` where the first argument is an expression that varies per test case; when the literal is the known-good value it should be the first (expected) argument.

**Test**
Introduce a deliberate bug in `CodeBuffer` and confirm the failure message correctly reads "expected: <good-value> but was: <broken-value>" rather than the reverse.
