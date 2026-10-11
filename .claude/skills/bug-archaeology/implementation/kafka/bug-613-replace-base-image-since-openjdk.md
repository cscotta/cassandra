**Commit**: 69e1c9101c · **Ticket**: KAFKA-19876
**Subsystem**: tests/docker
**Category**: Logic error in condition

**Root cause**
The openjdk image has been officially deprecated (ref:
https://hub.docker.com/_/openjdk  ), so we’ll need to switch to another
base image for our E2E test environment. I tested this on my local machine using the following command:
```
jdk_version="sapmachine:17-jdk-ubuntu-jammy" \
image_name="ducker-ak:$(git rev-parse --short HEAD)" \
TC_PATHS="tests/kafkatest/tests/client/pluggable_test.py" \
/bin/bash tests/docker/run_tests.sh
```
result:
```
[INFO:2025-11-14 14:40:35,072]: RunnerClient:
kafkatest.tests.client.pluggable_test.PluggableConsumerTest.test_start_stop.metadata_quorum=ISOLATED_KRAFT:
PASS
[WARNING - 2025-11-14 14:40:35,072 - runner_client - log - lineno:459]:
RunnerClient:
kafkat...

**Before/After**
```java
// Before
# The default OpenJDK base image.
# The base image of openjdk:17 is typically oraclelinux:8-slim, which doesn't include apt-get.
# Therefore, use openjdk:17-bullseye instead.
default_jdk="openjdk:17-bullseye"

// After
# The default JDK base image with apt-get support.
# The openjdk image has been officially deprecated. For more imformation, see: https://hub.docker.com/_/openjdk
default_jdk="sapmachine:17-jdk-ubuntu-jammy"
```

**How to spot**
When reviewing tests/docker code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19876 replace the base image since openjdk image is deprecated (#20886).

**Test**
Test modified in ducker-ak covering the corrected behaviour.
