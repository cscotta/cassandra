**Commit**: da6a562f6d · **Ticket**: KAFKA-17834
**Subsystem**: docker/jvm
**Category**: Wrong serialization / deserialization

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
      maintainer="Apache Kafka"
      maintainer="Apache Kafka"
MAINTAINER Apache Kafka dev@kafka.apache.org
ENV DEBIAN_FRONTEND noninteractive
CMD sudo service ssh start && tail -f /dev/null

// After
      org.opencontainers.image.authors="Apache Kafka"
      org.opencontainers.image.authors="Apache Kafka"
LABEL org.opencontainers.image.authors="Apache Kafka dev@kafka.apache.org"
ENV DEBIAN_FRONTEND=noninteractive
CMD ["sudo", "service", "ssh", "start", "-D"]
```

**How to spot**
When reviewing docker/jvm code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17834: improvements to dockerfile (#17554).

**Test**
Test modified in Dockerfile covering the corrected behaviour.
