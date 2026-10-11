## Commit

- Hash: b0b54f6db1
- Tickets: KAFKA-18466
- Subject: KAFKA-18466: Move log4j2 from implementation to compileOnly/testImplementation scope

## Subsystem

build / dependency management

## Category

Resource leak

## Root cause

`build.gradle` declared `log4j2` libraries in the `implementation` scope, which caused them to be bundled into the production JAR and exported as transitive dependencies to downstream consumers. Applications that already brought their own logging implementation would receive an unexpected duplicate binding, causing classloader conflicts or the wrong logging backend being activated.

## Before

```groovy
implementation "org.apache.logging.log4j:log4j-api:..."
implementation "org.apache.logging.log4j:log4j-core:..."
```

## After

```groovy
compileOnly "org.apache.logging.log4j:log4j-api:..."
testImplementation "org.apache.logging.log4j:log4j-core:..."
releaseOnly "org.apache.logging.log4j:log4j-core:..."  // only in distribution package
```

## How to spot

Logging framework jars (log4j, logback, slf4j-impl) in `implementation` scope rather than `compileOnly`/`runtimeOnly` — they should never be forced on downstream consumers.

## Test

Dependency scope verified via `./gradlew dependencies`; no functional test needed.
