**Commit**: 9cb49092f0 · **Ticket**: KAFKA-17653
**Subsystem**: LICENSE-binary
**Category**: Logic error in condition

**Root cause**
Scala compatibility table: https://docs.scala-lang.org/overviews/jdk-compatibility/overview.html#scala-compatibility-table

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
scala-library-2.13.14
scala-reflect-2.13.14
  SCALA_VERSION=2.13.14
  set SCALA_VERSION=2.13.14
scalaVersion=2.13.14
def defaultScala213Version = '2.13.14'

// After
scala-library-2.13.15
scala-reflect-2.13.15
  SCALA_VERSION=2.13.15
  set SCALA_VERSION=2.13.15
scalaVersion=2.13.15
def defaultScala213Version = '2.13.15'
```

**How to spot**
When reviewing LICENSE-binary code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17653 update scala version to 2.13.15 for support java 23 (#17314).

**Test**
No test added.
