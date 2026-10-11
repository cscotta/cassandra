**Commit**: 25f8bf82f7 · **Ticket**: KAFKA-18617
**Subsystem**: test-common/test-common-internal-api
**Category**: Logic error in condition

**Root cause**
Move the ClusterTest setup callback to `BeforeEachCallback` instead of
`BeforeTestExecutionCallback`. This allows the setup to happen slightly
earlier and before any `@BeforeEach` methods are invoked. We cannot inject ClusterInstance into a `@BeforeEach` method, but we can
still use the constructor injection which allows a `@BeforeEach` fixture
to access the ClusterInstance. This enables tests to do some common
cluster setup (like creating topics) before each test case.

**Before/After**
```java
// Before
A new `@ClusterTest` annotation is introduced which allows for a test to declaratively configure an underlying Kafka cluster.
```scala
@ClusterTest
def testSomething(): Unit = { ... }
This annotation has fields for a set of cluster types and number of brokers, as well as commonly parameterized configurations.
Arbitrary server properties can also be provided in the annotation:
@ClusterTest(
  types = {Type.KRAFT},
  brokerSecurityProtocol = SecurityProtocol.PLAINTEXT,
  properties = {
    @ClusterProperty(key = "inter.broker.protocol.version", value = "2.7-IV2"),
    @ClusterProperty(key = "socket.send.buffer.bytes", value = "10240"),

// After
Three annotations are provided for defining a template of a Kafka cluster.
* `@ClusterTest`: declarative style cluster definition
* `@ClusterTests`: wrapper around multiple `@ClusterTest`-s
* `@ClusterTemplate`: points to a function for imperative cluster definition

Another helper annotation `@ClusterTestDefaults` allows overriding the defaults for
all `@ClusterTest` in a single test class.

# Usage

The simplest usage is `@ClusterTest` by itself which will use some reasonable defaults.

```

**How to spot**
When reviewing test-common/test-common-internal-api code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18617 allow use of clusterinstance inside beforeeach (#18662).

**Test**
Test modified in README.md covering the corrected behaviour.
