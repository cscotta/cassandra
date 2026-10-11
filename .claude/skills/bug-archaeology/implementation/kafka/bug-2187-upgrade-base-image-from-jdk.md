**Commit**: 05a6898610 · **Ticket**: KAFKA-17812
**Subsystem**: tests/README.md
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
image_name="ducker-ak-openjdk-11" bash tests/docker/run_tests.sh
bash tests/docker/ducker-ak up -j 'openjdk:11'; tests/docker/run_tests.sh
ARG jdk_version=openjdk:11
default_jdk="openjdk:11"

// After
image_name="ducker-ak-openjdk:17-buster" bash tests/docker/run_tests.sh
bash tests/docker/ducker-ak up -j 'openjdk:17-buster'; tests/docker/run_tests.sh
# The base image of openjdk:17 is typically oraclelinux:8-slim, which doesn't include apt-get.
# Therefore, use openjdk:17-buster instead.
ARG jdk_version=openjdk:17-buster
default_jdk="openjdk:17-buster"
```

**How to spot**
When reviewing tests/README.md code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17812 upgrade base image of e2e from jdk 11 to jdk 17 (#17520).

**Test**
Test modified in README.md covering the corrected behaviour.
