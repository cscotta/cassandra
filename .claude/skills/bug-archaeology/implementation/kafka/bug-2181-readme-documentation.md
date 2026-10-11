**Commit**: e01f6a5c1e · **Ticket**: KAFKA-17740
**Subsystem**: README.md
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
We build and test Apache Kafka with Java 8, 11, 17 and 21. We set the `release` parameter in javac and scalac
to `8` to ensure the generated binaries are compatible with Java 8 or higher (independently of the Java version
used for compilation). Java 8 support project-wide has been deprecated since Apache Kafka 3.0, Java 11 support for
the broker and tools has been deprecated since Apache Kafka 3.7 and removal of both is planned for Apache Kafka 4.0 (
see [KIP-750](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=181308223) and
[KIP-1013](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=284789510) for more details).
**Please note that `./gradlew spotlessCheck` currently has an issue with Java 21 (see https://github.com/diffplug/spotless/pull/1920), so make sure to run this with JDK 11 or 17**

The import order is a part of static check. please call `spotlessApply` (require JDK 11+) to optimize the imports of Java codes before filing pull request.
**Please note that `./gradlew spotlessApply` currently has an issue with Java 21 (see https://github.com/diffplug/spotless/pull/1920), so make sure to run this with JDK 11 or 17**

  Java 8, Java 11, Java 17, and Java 21 are supported.

// After
We build and test Apache Kafka with 11, 17 and 21. We set the `release` parameter in javac and scalac
to `11` to ensure the generated binaries are compatible with Java 11 or higher (independently of the Java version
used for compilation). Java 11 support for the broker and tools has been deprecated since Apache Kafka 3.7 and removal
of both is planned for Apache Kafka 4.0.([KIP-1013](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=284789510) for more details).
The import order is a part of static check. please call `spotlessApply` to optimize the imports of Java codes before filing pull request.
  Java 11, Java 17, and Java 21 are supported.
  Note that Java 11 support for the broker and tools has been deprecated since Apache Kafka 3.7. Both will be removed in Apache Kafka 4.0.
        <p class="note">NOTE: Your local environment must have Java 11+ installed.</p>
                <li>
                    Java 8 support has been removed in Apache Kafka 4.0
                    See <a href="https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=181308223">KIP-750</a> for more details
                </li>
```

**How to spot**
When reviewing README.md code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17740 update readme and documentation (#17435).

**Test**
No test added.
