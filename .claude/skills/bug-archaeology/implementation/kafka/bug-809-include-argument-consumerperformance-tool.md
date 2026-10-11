**Commit**: f97b95c60a · **Ticket**: KAFKA-19498
**Subsystem**: docs/upgrade.html
**Category**: Logic error in condition

**Root cause**
This patch adds the include argument to ConsumerPerformance tool. ConsoleConsumer and ConsumerPerformance serve different purposes but
share common functionality for message consumption. Currently, there's
an inconsistency in their command-line interfaces:

- ConsoleConsumer supports an --include argument that allows users to
specify a regular expression pattern to filter topics for consumption
- ConsumerPerformance lacks this topic filtering capability, requiring
users to specify a single topic explicitly via --topic argument

This inconsistency creates two problems:

- Similar tools should provide similar topic selection capabilities for
better user experience
- Users cannot test consumer ...

**Before/After**
```java
// Before
// (no removals)

// After
    <li>
        The <code>ConsumerPerformance</code> command line tool has a new <code>include</code> option that is alternative to the <code>topic</code> option.
        This new option allows to pass a regular expression specifying a list of topics to include for consumption, which is useful to test consumer performance across multiple topics or dynamically matching topic sets.
    </li>
import java.util.Arrays;
import java.util.stream.Collectors;
    /**
     * Check that exactly one of a set of mutually exclusive arguments is present.
     */
    public static void checkOneOfArgs(OptionParser parser, OptionSet options, OptionSpec<?>... optionSpecs) {
        if (optionSpecs == null || optionSpecs.length == 0) {
            throw new IllegalArgumentException("At least one option must be provided");
```

**How to spot**
When reviewing docs/upgrade.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19498 add include argument to consumerperformance tool (#20221).

**Test**
Test modified in CommandLineUtilsTest.java covering the corrected behaviour.
