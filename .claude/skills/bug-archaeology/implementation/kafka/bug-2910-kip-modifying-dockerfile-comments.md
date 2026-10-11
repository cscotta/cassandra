**Commit**: 557c232675 · **Ticket**: KAFKA-16373
**Subsystem**: docker/prepare_docker_official_image_source.py
**Category**: Logic error in condition

**Root cause**
This PR aims to add the replace the dockerfile comments under the jvm/dockerfile  Dockerfile, with updated url. The original comment read
# Get kafka from https://archive.apache.org/dist/kafka and pass the url through build arguments . For DOI dockerfiles, we replace this with
# Get Kafka from https://downloads.apache.org/kafka, url passed as env var, for version {kafka_version} . Reviewers: Manikumar Reddy <manikumar.reddy@gmail.com>, Vedarth Sharma <vesharma@confluent.io>

**Before/After**
```java
// Before
def remove_args_and_hardcode_values(file_path, kafka_url):
        os.path.join(new_dir, args.kafka_version, args.image_type, 'Dockerfile'), kafka_url)

// After
import re
def remove_args_and_hardcode_values(file_path, kafka_version, kafka_url):
    original_comment = re.compile(r"# Get kafka from https://archive.apache.org/dist/kafka and pass the url through build arguments")
    updated_comment = f"# Get Kafka from https://downloads.apache.org/kafka, url passed as env var, for version {kafka_version}"
    filedata = original_comment.sub(updated_comment, filedata)
        os.path.join(new_dir, args.kafka_version, args.image_type, 'Dockerfile'), args.kafka_version, kafka_url)
```

**How to spot**
When reviewing docker/prepare_docker_official_image_source.py code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: [minor] kafka-16373: kip-1028: modifying dockerfile comments (#16261).

**Test**
No test added.
