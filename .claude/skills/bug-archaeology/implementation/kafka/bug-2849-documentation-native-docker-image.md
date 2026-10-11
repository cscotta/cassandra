**Commit**: bcf781230e · **Ticket**: KAFKA-16932
**Subsystem**: docker/README.md
**Category**: Logic error in condition

**Root cause**
This PR contains the the following documentation changes for the native docker image:

in the docker/README.md: How to build, release and promote the native docker image. in the tests/README.md: How to run system tests by bringing up kafka in the native mode. added docker/native/README.md
added html changes for the kafka-site
added native docker image support in the docker compose files examples. Testing:
Tested all the docker compose files with both the docker images - jvm and native
Tested the html changes locally with the kafka-site

Reviewers: Manikumar Reddy <manikumar.reddy@gmail.com>, Vedarth Sharma <vesharma@confluent.io>

**Before/After**
```java
// Before
This directory contains scripts to build, test, push and promote docker image for kafka. It also contains scripts to build, test the JVM-based Docker Official Image, and generate a PR template for the same to be raised under the Docker official images repo.
All of the steps can be either performed locally or by using Github Actions.
- image-type - This is the type of image that we intend to build. This will be dropdown menu type selection in the workflow. `jvm` image type is for official docker image (to be hosted on apache/kafka) as described in [KIP-975](https://cwiki.apache.org/confluence/display/KAFKA/KIP-975%3A+Docker+Image+for+Apache+Kafka)
- Example:-

- Example:-
- Example:-

// After
This directory contains:
- Scripts to build, test, push and promote docker images for apache kafka - both `jvm` based and graalvm based `native` kafka
- Scripts to build, test the JVM-based Docker Official Image, and generate a PR template for the same to be raised under the Docker official images repo.
  All of the steps can be either performed locally or by using Github Actions.
- image-type - This is the type of image that we intend to build. This will be dropdown menu type selection in the workflow.
  - `jvm` image type is for official docker image (to be hosted on apache/kafka) as described in [KIP-975](https://cwiki.apache.org/confluence/display/KAFKA/KIP-975%3A+Docker+Image+for+Apache+Kafka)
  - `native` image type is for graalvm based `native` kafka docker image (to be hosted on apache/kafka-native) as described in [KIP-974](https://cwiki.apache.org/confluence/display/KAFKA/KIP-974%3A+Docker+Image+for+GraalVM+based+Native+Kafka+Broker#KIP974:DockerImageforGraalVMbasedNativeKafkaBroker-ImageNaming)
- Example(jvm):-
- Example(native):-
  To build and test a native image type ensuring kafka to be containerised should be https://archive.apache.org/dist/kafka/3.8.0/kafka_2.13-3.8.0.tgz (it is recommended to use scala 2.13 binary tarball), following inputs in github actions workflow are recommended.
```
image_type: native
```

**How to spot**
When reviewing docker/README.md code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16932: add documentation for the native docker image (#16338).

**Test**
Test modified in README.md covering the corrected behaviour.
