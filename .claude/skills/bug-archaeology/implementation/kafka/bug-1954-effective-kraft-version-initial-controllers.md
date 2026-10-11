**Commit**: 98d47f47ef · **Ticket**: KAFKA-18028
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    if (!namespace.getBoolean("no_initial_controllers")) {
        if (config.quorumConfig.voters().isEmpty) {
          if (formatter.initialVoters().isEmpty()) {
            throw new TerseFailure("Because " + QuorumConfig.QUORUM_VOTERS_CONFIG +
              " is not set on this controller, you must specify one of the following: " +
              "--standalone, --initial-controllers, or --no-initial-controllers.");
          }
  @Test
  def testFormatWithNoInitialControllersSucceedsOnController(): Unit = {
    assertEquals(0, runFormatCommand(stream, properties,
      Seq("--no-initial-controllers", "--release-version", "3.9-IV0")))
    private Map<String, Short> featureLevels = new TreeMap<>();

// After
    if (namespace.getBoolean("no_initial_controllers")) {
      formatter.setNoInitialControllersFlag(true)
    } else {
        if (config.quorumConfig.voters().isEmpty && formatter.initialVoters().isEmpty) {
          throw new TerseFailure("Because " + QuorumConfig.QUORUM_VOTERS_CONFIG +
            " is not set on this controller, you must specify one of the following: " +
            "--standalone, --initial-controllers, or --no-initial-controllers.");
  @ParameterizedTest
  @ValueSource(booleans = Array(false, true))
  def testFormatWithNoInitialControllersSucceedsOnController(setKraftVersionFeature: Boolean): Unit = {
    val arguments = ListBuffer[String]("--release-version", "3.9-IV0", "--no-initial-controllers")
    if (setKraftVersionFeature) {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18028 the effective kraft version of --no-initial-controllers should be 1 rather than 0 (#17836).

**Test**
Test modified in StorageToolTest.scala covering the corrected behaviour.
