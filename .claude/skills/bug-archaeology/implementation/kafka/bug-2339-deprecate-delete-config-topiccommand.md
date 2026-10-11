**Commit**: 84fee3399e · **Ticket**: KAFKA-17087
**Subsystem**: docs/upgrade.html
**Category**: Logic error in condition

**Root cause**
Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            deleteConfigOpt = parser.accepts("delete-config", "This option is no longer supported.")
            CommandLineUtils.checkInvalidArgs(parser, options, deleteConfigOpt,
                invalidOptions(new HashSet<>(Arrays.asList(bootstrapServerOpt)), Arrays.asList(alterOpt)));
        assertInitializeInvalidOptionsExitCode(1,
            new String[] {"--bootstrap-server", bootstrapServer, "--alter", "--topic", topicName,
                "--partitions", "3", "--delete-config", "cleanup.policy"});

// After
            <li>
                The <code>--delete-config</code> option in the <code>kafka-topics</code> command line tool has been deprecated.
            </li>
        /**
         * @deprecated since 4.0 and should not be used any longer.
         */
        @Deprecated
            deleteConfigOpt = parser.accepts("delete-config", "This option is no longer supported and has been deprecated since 4.0")
            if (has(deleteConfigOpt)) {
                System.err.println("delete-config option is no longer supported and deprecated since version 4.0. The config will be fully removed in future releases.");
            }

```

**How to spot**
When reviewing docs/upgrade.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17087 deprecate `delete-config` of topiccommand (#17222).

**Test**
Test modified in TopicCommandTest.java covering the corrected behaviour.
