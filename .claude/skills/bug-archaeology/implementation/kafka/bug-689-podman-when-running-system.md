**Commit**: 125cd95c6b · **Ticket**: KAFKA-19745
**Subsystem**: tests/README.md
**Category**: Logic error in condition

**Root cause**
Co-authored-by: Federico Valeri <fedevaleri@gmail.com>
Reviewers: Mickael Maison <mickael.maison@gmail.com>, Federico Valeri <fedevaleri@gmail.com>

**Before/After**
```java
// Before
    echo_and_do docker build --memory="${docker_build_memory_limit}" \
    must_do -v docker run --privileged \
        --memory=${docker_run_memory_limit} --memory-swappiness=1 \
    local running_container="$(docker ps -f=network=ducknet -q)"
    must_do -v -o docker cp "${custom_ducktape}" "${running_container}:/opt/ducktape"
    docker exec --user=root ducker01 bash -c 'set -x && cd /opt/kafka-dev/tests && sudo python3 ./setup.py develop install && cd /opt/ducktape && sudo python3 ./setup.py develop install'

// After
  - You can use also use Podman as an alternative to Docker, see below.
  * Set container runtime (docker or podman)
  ```
  CONTAINER_RUNTIME="podman" bash tests/docker/run_tests.sh
  ```
By default, the test scripts will auto-detect the available container runtime (prioritizing docker over podman).
You can explicitly set it to `docker` or `podman` using this environment variable.
# Container runtime command (docker or podman)
container_runtime=""

Supports Docker and Podman container runtimes. Auto-detects available runtime
or can be overridden with CONTAINER_RUNTIME environment variable.
```

**How to spot**
When reviewing tests/README.md code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19745: add support for podman when running system tests (#20546).

**Test**
Test modified in README.md covering the corrected behaviour.
