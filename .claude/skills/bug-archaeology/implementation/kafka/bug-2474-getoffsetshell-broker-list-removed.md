**Commit**: 165076afc6 · **Ticket**: KAFKA-17390
**Subsystem**: tools/GetOffsetShell
**Category**: Wrong constant / default value

**Root cause**
`GetOffsetShell` accepted `--broker-list` as a deprecated alternative to `--bootstrap-server` via `requiredUnless("broker-list")`. This kept the deprecated option alive indefinitely, polluted the help text, and required internal `effectiveBrokerListOpt` bookkeeping to track which option was active. In 4.0, the tool should accept only `--bootstrap-server` and give a clear parse error if the old flag is used.

**Before/After**
```java
// Before
OptionSpec<String> brokerListOpt = parser.accepts("broker-list", "DEPRECATED...").withRequiredArg();
OptionSpec<String> bootstrapServerOpt = parser.accepts("bootstrap-server", "REQUIRED.")
    .requiredUnless("broker-list");
// runtime: effectiveBrokerListOpt = has(bootstrapServerOpt) ? bootstrapServerOpt : brokerListOpt

// After
bootstrapServerOpt = parser.accepts("bootstrap-server", "REQUIRED.").withRequiredArg();
// broker-list option removed entirely
```

**How to spot**
Search CLI option parsers for `requiredUnless` — each such call indicates a legacy option that is still quietly supported. During a major version release, replace every `requiredUnless` pattern with a straightforward required option and remove the deprecated alternative.

**Test**
No test added.
