**Commit**: 767a62ade6 · **Ticket**: KAFKA-18737
**Subsystem**: core/src/main/scala/kafka/docker/KafkaDockerWrapper.scala
**Category**: Wrong constant / default value

**Root cause**
The Docker image's `KafkaDockerWrapper` called `StorageTool.main()` with a `--standalone` flag that implies dynamic voters (KRaft 4.0 style). The Docker Hub image and documentation were built around static voter configurations (`controller.quorum.voters` in a properties file), which require the storage format command to be run without `--standalone`. This mismatch caused the format command to fail for Docker users following the published documentation. The fix removes `--standalone` from the format command array and adds an error-handling wrapper that provides a user-friendly message when the quorum-voters config key appears in the error.

**Before/After**
```scala
// BEFORE – --standalone implies dynamic voters, breaks static-voter configs
Array("format", "--cluster-id=" + clusterId, "-c",
      s"${configsPath}/server.properties", "--standalone")

// AFTER – no --standalone; static voter config in dedicated properties file
Array("format", "--cluster-id=" + clusterId, "-c",
      s"${configsPath}/server.properties")
```

**How to spot**
Default CLI argument arrays that carry mode-selecting flags should be reviewed whenever the deployment model changes (e.g., ZK → KRaft, static → dynamic voters). A flag that is correct for the new default may break existing deployment configurations that relied on the old behaviour.

**Test**
`KafkaDockerWrapperTest` expected command assertion updated to match the new argument array without `--standalone`.
