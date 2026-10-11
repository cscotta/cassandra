**Commit**: 99fafc44ba · **Ticket**: KAFKA-19876

**Subsystem**: docker/

**Category**: Wrong constant/default value

**Root cause**
The Docker base image for Kafka was switched from `sapmachine` to `eclipse-temurin` so that older Kafka versions (e.g. 3.9) that require JDK 8 can use the same image provider. `sapmachine` does not publish JDK 8 images, so Docker builds for those versions would fail. Additionally, `eclipse-temurin` places the JDK in a non-standard path, requiring a symlink under `~/.ssh/environment` and `PermitUserEnvironment yes` in `sshd_config` so that the `java` binary is found correctly over SSH sessions.

**Before**
```dockerfile
# Before
FROM sapmachine:...
```

**After**
```dockerfile
# After
FROM eclipse-temurin:...
# + symlink to fix java path for SSH sessions
```

**How to spot**
Check `docker/` for a `FROM` directive pointing to `sapmachine`; if the project needs to support JDK 8, `sapmachine` cannot be used because it only publishes JDK 11 and later images.

**Test**
Build the Docker image for Kafka 3.9 (which requires JDK 8) using the `eclipse-temurin` base and confirm that `java -version` succeeds both in the container shell and over an SSH session.
