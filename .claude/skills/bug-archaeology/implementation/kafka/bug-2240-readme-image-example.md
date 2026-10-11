**Commit**: 7592bc3cbe · **Ticket**: KAFKA-17655
**Subsystem**: tests/docs (README.md)
**Category**: Missing null / bounds check

**Root cause**
The system-test README lacked an example showing how to run tests against a specific Docker image by name. Operators frequently need to test against a pre-built image tag rather than building from source; the absence of the example caused repeated questions and incorrect invocations that wasted CI time.

**Before/After**
```markdown
<!-- Before – no image-name example -->

<!-- After – example added -->
To run tests with a specific image:
  ducker-ak run --image <image-name> kafkatest.tests.foo.Bar
```

**How to spot**
When a recurring support question involves a non-obvious CLI flag, add a concrete example to the relevant README section so the answer is discoverable without filing an issue.

**Test**
No test added.
