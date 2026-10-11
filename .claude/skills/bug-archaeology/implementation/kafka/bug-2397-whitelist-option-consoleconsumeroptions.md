**Commit**: 60707a5fe9 · **Ticket**: KAFKA-17392
**Subsystem**: docs/upgrade.html
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    private final OptionSpec<String> whitelistOpt;
        whitelistOpt = parser.accepts("whitelist",
                        "DEPRECATED, use --include instead; ignored if --include specified. Regular expression specifying list of topics to include for consumption.")
                .withRequiredArg()
                .describedAs("Java regex (String)")
                .ofType(String.class);
        // user need to specify value for either --topic or one of the include filters options (--include or --whitelist)
            CommandLineUtils.printUsageAndExit(parser, "Exactly one of --include/--topic is required. " +
                    (options.has(whitelistOpt) ? "--whitelist is DEPRECATED use --include instead; ignored if --include specified." : ""));
                : Optional.ofNullable(options.valueOf(whitelistOpt));
    @Test
    public void shouldParseWhitelistArgument() throws IOException {

// After
            <li>
                The <code>--whitelist</code> option was removed from the <code>kafka-console-consumer</code> command line tool.
                Please use <code>--include</code> instead.
            </li>
        // user need to specify value for either --topic or --include options)
            CommandLineUtils.printUsageAndExit(parser, "Exactly one of --include/--topic is required. ");
                : Optional.empty();
```

**How to spot**
When reviewing docs/upgrade.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17392 remove whitelist option in consoleconsumeroptions (#17138).

**Test**
Test modified in ConsoleConsumerOptionsTest.java covering the corrected behaviour.
