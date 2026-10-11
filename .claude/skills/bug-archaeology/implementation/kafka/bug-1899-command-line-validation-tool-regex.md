**Commit**: c3506834e9 · **Ticket**: KAFKA-17598
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
This patch introduces the `--validate-regex` argument to the `kafka-consumer-group` command line tool as defined in KIP-848. The new argument allows the verification of RE2 regular expressions. Reviewers: Andrew Schofield <aschofield@confluent.io>, Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
            // should have exactly one action
            long actions = Stream.of(opts.listOpt, opts.describeOpt, opts.deleteOpt, opts.resetOffsetsOpt, opts.deleteOffsetsOpt).filter(opts.options::has).count();
            if (actions != 1)
                CommandLineUtils.printUsageAndExit(opts.parser, "Command must include exactly one action: --list, --describe, --delete, --reset-offsets, --delete-offsets");
    private static final

// After
    implementation libs.re2j
        <allow pkg="com.google.re2j"/>
import com.google.re2j.Pattern;
import com.google.re2j.PatternSyntaxException;
import joptsimple.OptionSpec;
            List<OptionSpec<?>> actions = List.of(
                opts.listOpt,
                opts.describeOpt,
                opts.deleteOpt,
                opts.resetOffsetsOpt,
                opts.deleteOffsetsOpt,
                opts.validateRegexOpt
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17598; command line validation tool for re2j regex (#18031).

**Test**
Test modified in ConsumerGroupCommandTest.java covering the corrected behaviour.
