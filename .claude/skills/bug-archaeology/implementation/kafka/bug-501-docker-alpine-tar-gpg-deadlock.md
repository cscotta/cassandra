## Bug 501 — Docker Alpine: tar extraction deadlocked by pre-loaded gcompat

**Commit:** 41b33d82c9
**Ticket:** (no JIRA – Docker fix)
**Subsystem:** docker / jvm
**Category:** Deadlock

### Root cause
The Alpine-based JVM Dockerfile used `RUN set -o nounset; apk add gcompat …` and then, in the same shell block, called `tar xfz jdk.tar.gz` **before** calling `gpg --verify`. The `gcompat` package installs a GNU libc compatibility shim that intercepts certain syscalls. When `gcompat` is loaded into the process space prior to the `tar` invocation, `tar`'s internal threading model dead-locked against gcompat's loader hooks on this Alpine kernel version, hanging the build step indefinitely.

### Before
```dockerfile
RUN apk add gcompat curl gnupg tar \
 && gpg --verify jdk.tar.gz.sig jdk.tar.gz \
 && tar xfz jdk.tar.gz
```

### After
```dockerfile
RUN apk add curl gnupg tar \
 && gpg --verify jdk.tar.gz.sig jdk.tar.gz \
 && apk add gcompat \
 && tar xfz jdk.tar.gz
```

### How to spot
Any Dockerfile that installs `gcompat` (a GNU libc compatibility shim for Alpine) in the same `RUN` layer as `tar`, where `gcompat` is added **before** the `tar` call, is a candidate. Symptom is an indefinitely hanging Docker build step.

### Test
Manual Docker build on Alpine. No automated unit test; the CI image build pipeline serves as the regression gate.
