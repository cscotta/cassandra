**Commit**: 5a2d79a1ff · **Ticket**: KAFKA-20359
**Subsystem**: docker/docker_official_images
**Category**: Logic error in condition

**Root cause**
Updating docker image with fully qualified name
`docker.io/library/eclipse-temurin:21-jdk-jammy`

Reviewers: TaiJuWu <tjwu1217@gmail.com>, PoAn Yang <payang@apache.org>,
 Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
FROM eclipse-temurin:21-jre-alpine AS build-jsa
FROM eclipse-temurin:21-jre-alpine
FROM eclipse-temurin:21-jre-alpine AS build-jsa
FROM eclipse-temurin:21-jre-alpine
default_jdk="eclipse-temurin:17-jdk-jammy"
    If -j|--jdk is specified, you can customize the OpenJDK base image used for building
    the ducker container. Defaults to ${default_jdk}. Example: -j openjdk:17-bullseye

// After
FROM docker.io/library/eclipse-temurin:21-jre-alpine AS build-jsa
FROM docker.io/library/eclipse-temurin:21-jre-alpine
FROM docker.io/library/eclipse-temurin:21-jre-alpine AS build-jsa
FROM docker.io/library/eclipse-temurin:21-jre-alpine
default_jdk="docker.io/library/eclipse-temurin:17-jdk-jammy"
    If -j|--jdk is specified, you can customize the JDK base image used for building
    the ducker container. Defaults to ${default_jdk}. Example: -j docker.io/library/eclipse-temurin:17-jdk-jammy
```

**How to spot**
When reviewing docker/docker_official_images code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20359 update docker image with fully qualified name (#21877).

**Test**
Test modified in ducker-ak covering the corrected behaviour.
