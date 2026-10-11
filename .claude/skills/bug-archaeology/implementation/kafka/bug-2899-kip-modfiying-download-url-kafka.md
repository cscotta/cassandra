**Commit**: b7dcae44ff · **Ticket**: KAFKA-16373
**Subsystem**: docker/docker_official_images
**Category**: Logic error in condition

**Root cause**
This PR modifies the download url from https://downloads.apache.org/kafka/ to https://archive.apache.org/dist/kafka/ as the former is not permanent. Reviewers: Manikumar Reddy <manikumar.reddy@gmail.com>, Vedarth Sharma <vesharma@confluent.io>

**Before/After**
```java
// Before
    kafka_url = f"https://downloads.apache.org/kafka/{args.kafka_version}/kafka_2.13-{args.kafka_version}.tgz"
# Get Kafka from https://downloads.apache.org/kafka, url passed as env var, for version 3.7.0
ENV kafka_url https://downloads.apache.org/kafka/3.7.0/kafka_2.13-3.7.0.tgz
# Get Kafka from https://downloads.apache.org/kafka, url passed as env var, for version 3.7.0
ENV kafka_url https://downloads.apache.org/kafka/3.7.0/kafka_2.13-3.7.0.tgz
ENV build_date 2024-06-10
    updated_comment = f"# Get Kafka from https://downloads.apache.org/kafka, url passed as env var, for version {kafka_version}"
    kafka_url = f"https://downloads.apache.org/kafka/{args.kafka_version}/kafka_2.13-{args.kafka_version}.tgz"

// After
    kafka_url = f"https://archive.apache.org/dist/kafka/{args.kafka_version}/kafka_2.13-{args.kafka_version}.tgz"
# Get Kafka from https://archive.apache.org/dist/kafka, url passed as env var, for version 3.7.0
ENV kafka_url https://archive.apache.org/dist/kafka/3.7.0/kafka_2.13-3.7.0.tgz
# Get Kafka from https://archive.apache.org/dist/kafka, url passed as env var, for version 3.7.0
ENV kafka_url https://archive.apache.org/dist/kafka/3.7.0/kafka_2.13-3.7.0.tgz
ENV build_date 2024-06-11
    updated_comment = f"# Get Kafka from https://archive.apache.org/dist/kafka, url passed as env var, for version {kafka_version}"
    kafka_url = f"https://archive.apache.org/dist/kafka/{args.kafka_version}/kafka_2.13-{args.kafka_version}.tgz"
```

**How to spot**
When reviewing docker/docker_official_images code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16373: kip-1028: modfiying download url for kafka dockerfile (#16281).

**Test**
Test modified in docker_official_image_build_test.py covering the corrected behaviour.
