**Commit**: e9ca0bb0f6 · **Ticket**: KAFKA-18983
**Subsystem**: README.md
**Category**: Logic error in condition

**Root cause**
There are few README not added because I am not sure if they need to be
mentioned in root README. ```
./test-common/test-common-internal-api/src/main/java/org/apache/kafka/common/test/api/README.md
./storage/src/test/java/org/apache/kafka/tiered/storage/README.md
./.github/workflows/README.md
./raft/README.md
./committer-tools/README.md
```

Reviewers: Ken Huang <s7133700@gmail.com>, TengYao Chi
<kitingiao@gmail.com>, PoAn Yang <payang@apache.org>, Jhen-Yung Hsu
<jhenyunghsu@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
- kafka-url - This is the url to download kafka tarball from. For example kafka tarball url from (https://archive.apache.org/dist/kafka). For building RC image this will be an RC tarball url.
  - `jvm` image type is for official docker image (to be hosted on apache/kafka) as described in [KIP-975](https://cwiki.apache.org/confluence/display/KAFKA/KIP-975%3A+Docker+Image+for+Apache+Kafka)
  - `native` image type is for graalvm based `native` kafka docker image (to be hosted on apache/kafka-native) as described in [KIP-974](https://cwiki.apache.org/confluence/display/KAFKA/KIP-974%3A+Docker+Image+for+GraalVM+based+Native+Kafka+Broker#KIP974:DockerImageforGraalVMbasedNativeKafkaBroker-ImageNaming)
- image-type - This is the type of image that we intend to build. This will be dropdown menu type selection in the workflow. `jvm` image type is for official docker image (to be hosted on apache/kafka) as described in [KIP-975](https://cwiki.apache.org/confluence/display/KAFKA/KIP-975%3A+Docker+Image+for+Apache+Kafka).
  - **NOTE:** As of now [KIP-1028](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1028%3A+Docker+Official+Image+for+Apache+Kafka) only aims to release JVM based Docker Official Images and not GraalVM based native Apache Kafka docker image.

// After
See [Apache Kafka Message Definitions](clients/src/main/resources/common/message/README.md) for details on Apache Kafka message protocol.

See [docker/README.md](docker/README.md) for detailed information.

### Using Trogdor for testing ###

We use Trogdor as a test framework for Apache Kafka. You can use it to run benchmarks and other workloads.

See [trogdor/README.md](trogdor/README.md).

### Release Kafka ###

```

**How to spot**
When reviewing README.md code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18983 ensure all readme.md(s) are mentioned by the root readme.md (#19420).

**Test**
No test added.
