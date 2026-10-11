**Commit**: ec17e5af0c · **Ticket**: KAFKA-20378
**Subsystem**: tests/docker
**Category**: Logic error in condition

**Root cause**
This PR adds a possibility to configure memory limit within system
tests. ### Why? Right now, anyone who needs to a different memory limit in CI has to do
something like:
```java
sed -i
's/docker_run_memory_limit="2000m"/docker_run_memory_limit="4000m"/'
tests/docker/ducker-ak
```
with is not so UX friendly and it can break anytime if anyone changes
the content of the `ducker-ak`. With such change we can easily do:
```bash
./tests/docker/ducker-ak up --memory 4000m
```
or even
```bash
DUCKER_RUN_MEMORY=4000m ./tests/docker/run_tests.sh
```
So there is no need for using sed etc.

**Before/After**
```java
// Before
docker_run_memory_limit="2000m"

// After
# Can be overridden by DUCKER_RUN_MEMORY environment variable or --memory flag.
default_docker_run_memory_limit="2000m"
        [--memory MEMORY_LIMIT]
    If --memory is specified, sets the memory limit for each container node.
    Defaults to ${default_docker_run_memory_limit}. Can also be set via the DUCKER_RUN_MEMORY
    environment variable. The --memory flag takes precedence over the environment variable.

            --memory) set_once docker_run_memory_limit "${2}" "the container memory limit"; shift 2;;
    [[ -n "${docker_run_memory_limit}" ]] || docker_run_memory_limit="${DUCKER_RUN_MEMORY:-${default_docker_run_memory_limit}}"
```

**How to spot**
When reviewing tests/docker code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20378 make container memory limit configurable (#21924).

**Test**
Test modified in ducker-ak covering the corrected behaviour.
