**Commit**: b2c1a0fb9f · **Ticket**: KAFKA-18841
**Subsystem**: docker/README.md
**Category**: Logic error in condition

**Root cause**
### Case 1: no --kafka-url and --kafka-archive

Should fail. One of argument (--kafka-url/--kafka-archive) is required. ```
> python docker_build_test.py apache/kafka --image-tag KAFKA-18841
--image-type jvm --build
usage: docker_build_test.py [-h] [--image-tag TAG] [--image-type
{jvm,native}] [--build] [--test] (--kafka-url KAFKA_URL |
--kafka-archive KAFKA_ARCHIVE) image
docker_build_test.py: error: one of the arguments --kafka-url/-u
--kafka-archive/-a is required
```

### Case 2: --kafka-url with native

```
> python docker_build_test.py apache/kafka --image-tag KAFKA-18841
--image-type native --kafka-url
https://dist.apache.org/repos/dist/dev/kafka/4.0.0-rc0/kafka_2.13-4.0.0.tgz
--build...

**Before/After**
```java
// Before
def build_docker_image_runner(command, image_type):
def build_docker_image(image, tag, kafka_url, image_type):
    image = f'{image}:{tag}'
    build_docker_image_runner(f"docker build -f $DOCKER_FILE -t {image} --build-arg kafka_url={kafka_url} --build-arg build_date={date.today()} $DOCKER_DIR", image_type)

    parser.add_argument("--kafka-url", "-u", dest="kafka_url", help="Kafka url to be used to download kafka binary tarball in the docker image")

// After
- Example(local build archive with jvm or native image type) :- To build and test an image named test with local build archive
```
python docker_build_test.py kafka/test --image-tag=local-build --image-type=<jvm/native> --kafka-archive=</absolute/path/to/core/build/distributions/kafka_2.13-4.1.0-SNAPSHOT.tgz>
```
def build_docker_image_runner(command, image_type, kafka_archive=None):
    if kafka_archive:
        copy_file(kafka_archive, f"{temp_dir_path}/{image_type}/kafka.tgz")
        docker_build_test.py <image_name> --image-tag <image_tag> --image-type <image_type> --kafka-archive <kafka_archive>
        <kafka_archive> can be passed as an alternative to <kafka_url> to use a local kafka archive. The path of kafka_archive should be absolute.
```

**How to spot**
When reviewing docker/README.md code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18841: enable to test docker image locally (#19028).

**Test**
Test modified in docker_build_test.py covering the corrected behaviour.
