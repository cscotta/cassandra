**Commit**: 70824be92a · **Ticket**: KAFKA-19501

**Subsystem**: Docker / Base image

**Category**: Dependency/version management

**Root cause**
The Kafka Docker image was based on `openjdk:17-buster` (Debian 10 Buster), which reached end-of-life and stopped receiving security patches. Upgrading to `openjdk:17-bullseye` (Debian 11 Bullseye) brings an actively maintained OS base with current security fixes while keeping the same JDK version, ensuring that containerized Kafka deployments do not run on an unpatched operating system.

**Before/After**
```dockerfile
# Before
FROM openjdk:17-buster

# After
FROM openjdk:17-bullseye
```

**How to spot**
Periodically audit `FROM` lines in all Dockerfiles for end-of-life base image tags. Tools like `docker scout` or Trivy will flag images built on EOL distributions. The Debian release schedule is publicly documented.

**Test**
CI builds the updated Dockerfile and runs the standard Kafka Docker integration tests to confirm the image starts correctly and all smoke tests pass with the Bullseye base.
