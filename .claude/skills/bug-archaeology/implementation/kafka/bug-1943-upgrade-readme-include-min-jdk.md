**Commit**: 8be99f44dc · **Ticket**: KAFKA-18097
**Subsystem**: README.md
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
We build and test Apache Kafka with 11, 17 and 21. We set the `release` parameter in javac and scalac
to `11` to ensure the generated binaries are compatible with Java 11 or higher (independently of the Java version
used for compilation). Java 11 support for the broker and tools has been deprecated since Apache Kafka 3.7 and removal
of both is planned for Apache Kafka 4.0.([KIP-1013](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=284789510) for more details).

// After
We build and test Apache Kafka with 17 and 23. The `release` parameter in javac and scalac is set to `11` for the clients
and streams modules, and `17` for the broker and tools, ensuring compatibility with their respective minimum Java versions.
```

**How to spot**
When reviewing README.md code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18097 upgrade readme to include min jdk changes (#17948).

**Test**
No test added.
