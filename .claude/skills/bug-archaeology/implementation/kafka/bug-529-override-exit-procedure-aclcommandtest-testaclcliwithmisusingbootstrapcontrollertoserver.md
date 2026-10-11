**Commit**: 19ac4ced88 · **Ticket**: KAFKA-20007
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
`AclCommandTest#testAclCliWithMisusingBootstrapControllerToServer`
crashes the Gradle executor as the command exits with a status code 1. This fix updates the exit procedure prior to invoking the command. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        return ToolsTestUtils.grabConsoleOutputAndError(() -> AclCommand.main(args.toArray(new String[0])));
        Exit.setExitProcedure((status, message) -> {
            if (status == 1)
                throw new RuntimeException("Exiting command");
            else
                throw new AssertionError("Unexpected exit with status " + status);
        });
        try {
            for (PatternType patternType : PatternType.values()) {
                List<String> addCmd = new ArrayList<>(cmdArgs);
                addCmd.addAll(List.of("--allow-principal", PRINCIPAL.toString(), PRODUCER, TOPIC, "Test",
                        ADD, RESOURCE_PATTERN_TYPE, patternType.toString()));

// After
        Exit.setExitProcedure((status, message) -> {
            if (status == 1)
                throw new RuntimeException("Exiting command");
            else
                throw new AssertionError("Unexpected exit with status " + status);
        });
        try {
            return ToolsTestUtils.grabConsoleOutputAndError(() -> AclCommand.main(args.toArray(new String[0])));
        } finally {
            Exit.resetExitProcedure();
        }
        for (PatternType patternType : PatternType.values()) {
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20007 override exit procedure in aclcommandtest#testaclcliwithmisusingbootstrapcontrollertoserver (#21173).

**Test**
Test modified in AclCommandTest.java covering the corrected behaviour.
