**Commit**: c36b993af0 · **Ticket**: KAFKA-17738
**Subsystem**: tests/docker
**Category**: Wrong constant / default value

**Root cause**
The system-test Docker base image used `openjdk:8`, which is end-of-life and incompatible with Kafka 4.0's minimum Java 11 requirement. E2E tests that installed JDK 8 inside the container would fail to start a 4.0 broker, masking real test failures behind environment setup errors. The `ducker-ak` script also contained JDK-8-specific path logic that needed updating.

**Before/After**
```dockerfile
# Before
FROM openjdk:8

# After
FROM openjdk:11
```

**How to spot**
Whenever `minJavaVersion` is bumped in `build.gradle`, immediately audit `tests/docker/Dockerfile` and any CI container definitions for matching version strings; stale base images silently produce a broken test environment rather than a clear build error.

**Test**
No test added.
