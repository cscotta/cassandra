## Commit
f93ffeb337

## Ticket
KAFKA-20360

## Subsystem
Testing infrastructure

## Category
Wrong constant / default value

## Root cause
System-test dependencies `ducktape` (0.13.0) and `requests` (<2.32.4) were pinned to versions with known bugs/vulnerabilities. Bumping ducktape to 0.14.0 and requests to ≥2.32.4 fixes flaky test behavior introduced by the older versions.

## Before
```
ducktape==0.13.0
requests>=2.32.2
```

## After
```
ducktape==0.14.0
requests>=2.32.4
```

## How to spot
When system tests produce intermittent failures on test framework operations (e.g., SSH, HTTP retries), check whether the ducktape or requests version has known regressions. Pin upgrades are straightforward but require validating the full system-test matrix.

## Test
CI system-test run with updated dependencies.
