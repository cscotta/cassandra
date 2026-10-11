**Commit**: 7200a5c012 · **Ticket**: KAFKA-19976
**Subsystem**: docs/apis
**Category**: Logic error in condition

**Root cause**
KIP-1244: https://cwiki.apache.org/confluence/x/r4LMFw

* Deprecate streams-scala module
* Add migration document

Reviewers: Matthias J. Sax <mjsax@apache.org>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before


// After

> **⚠️ DEPRECATION NOTICE**: The `kafka-streams-scala` library is deprecated as of Kafka 4.3
> and will be removed in Kafka 5.0. Please migrate to using the Java Streams API directly from Scala.
> See the [migration guide](/{version}/streams/developer-guide/scala-migration) for details.
  * The `kafka-streams-scala` library is deprecated as of Kafka 4.3 and will be removed in Kafka 5.0. For further details, please refer to the [migration guide](/{version}/streams/developer-guide/scala-migration).
> **⚠️ DEPRECATION NOTICE**: The Kafka Streams DSL for Scala library (`kafka-streams-scala`) is
> **deprecated as of Kafka 4.3** and will be **removed in Kafka 5.0**.
>
> **See the [migration guide](/{version}/streams/developer-guide/scala-migration)** for instructions
> and code examples showing how to migrate from the Scala wrapper to the Java API.

---
```

**How to spot**
When reviewing docs/apis code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19976 deprecate streams-scala module (#21480).

**Test**
No test added.
