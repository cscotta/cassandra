**Commit**: cf183274f5 · **Ticket**: KAFKA-19725
**Subsystem**: docs/upgrade.html
**Category**: Logic error in condition

**Root cause**
*What*
https://issues.apache.org/jira/browse/KAFKA-19725

- Implement KIP-1147 for ConsoleProducer where we replace --property
with --reader-property. - Currently the previous names for the options are still usable but
there will be warning message stating those are deprecated and will be
removed in a future version. - I have added unit tests and also manually verified using the console
tools that things are working as expected. Reviewers: Andrew Schofield <aschofield@confluent.io>, Jhen-Yung Hsu
 <jhenyunghsu@gmail.com>

**Before/After**
```java
// Before
        command = ["echo", f'"{key}:{value}"', "|", f"{self.FIXTURES_DIR}/{constants.KAFKA_CONSOLE_PRODUCER}", "--topic", topic, "--property", "'parse.key=true'", "--property", "'key.separator=:'", "--timeout", f"{constants.CLIENT_TIMEOUT}"]

// After
        command = ["echo", f'"{key}:{value}"', "|", f"{self.FIXTURES_DIR}/{constants.KAFKA_CONSOLE_PRODUCER}", "--topic", topic, "--reader-property", "'parse.key=true'", "--reader-property", "'key.separator=:'", "--timeout", f"{constants.CLIENT_TIMEOUT}"]
            <li>
                In <code>kafka-console-producer.sh</code>, the option <code>--property</code> which is used to specify properties
                in the form key=value to the message reader is deprecated in favor of <code>--reader-property</code>.
            </li>
        @Deprecated(since = "4.2", forRemoval = true)
        private OptionSpec<String> readerPropertyOpt;
                            "(DEPRECATED) A mechanism to pass user-defined properties in the form key=value to the message reader. This allows custom configuration for a user-defined message reader." +
                                    "\nDefault properties include:" +
                                    "\n parse.key=false" +
                                    "\n parse.headers=false" +
                                    "\n ignore.error=false" +
```

**How to spot**
When reviewing docs/upgrade.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19725: replace --property with --reader-property in consoleproducer (kip-1147) (#20673).

**Test**
Test modified in docker_sanity_test.py covering the corrected behaviour.
