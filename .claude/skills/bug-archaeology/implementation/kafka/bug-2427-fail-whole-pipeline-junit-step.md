**Commit**: 040ae26472 · **Ticket**: KAFKA-17479
**Subsystem**: .github/scripts
**Category**: Resource leak

**Root cause**
Fixes an issue where the CI workflow could appear to be successful in the event of a timeout and no failing tests. Instead of using Github Action's timeout, this patch makes use of the linux `timeout` command. This lets us capture the exit code and handle timeouts separately from a failed execution. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
def get_env(key: str) -> str:
    logger.debug(f"Read env {key}: {value}")
    return value
    logger.debug(summary)
    if total_failures > 0:
        logger.debug(f"Failing this step due to {total_failures} test failures")
        exit(1)
    elif total_errors > 0:
        logger.debug(f"Failing this step due to {total_errors} test errors")
        exit(0)
        timeout-minutes: 180  # 3 hours
        continue-on-error: true

// After
def get_env(key: str, fn = str) -> Optional:
    if value is None:
        logger.debug(f"Could not find env {key}")
        return None
    else:
        logger.debug(f"Read env {key}: {value}")
        return fn(value)
    # Print special message if there was a timeout
    exit_code = get_env("GRADLE_EXIT_CODE", int)
    if exit_code == 124:
        logger.debug(f"Gradle command timed out. These are partial results!")
        logger.debug(summary)
```

**How to spot**
When reviewing .github/scripts code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17479 fail the whole pipeline if junit step times out [4/n] (#17121).

**Test**
No test added.
