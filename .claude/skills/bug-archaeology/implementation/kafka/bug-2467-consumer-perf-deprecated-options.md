**Commit**: 4a3ab89f95 · **Ticket**: KAFKA-17386
**Subsystem**: tools/ConsumerPerformance
**Category**: Wrong constant / default value

**Root cause**
`ConsumerPerformance` accepted `--broker-list` as a deprecated alias for `--bootstrap-server`, and also accepted `--threads` and `--num-fetch-threads` as "deprecated and ignored" options. Keeping these options polluted the help text, caused confusion in scripts that still used them, and prevented the tool from giving a clear error when used with old invocation patterns. Additionally, the Python system-test service always passed `--broker-list` regardless of version, so version-aware flag selection was missing.

**Before/After**
```java
// Before
brokerListOpt = parser.accepts("broker-list", "DEPRECATED...").withRequiredArg();
bootstrapServerOpt = parser.accepts("bootstrap-server", "REQUIRED unless --broker-list...")
    .requiredUnless("broker-list");

// After
bootstrapServerOpt = parser.accepts("bootstrap-server", "REQUIRED: The server(s) to connect to.")
    .withRequiredArg();
// broker-list, threads, num-fetch-threads options removed
```

**How to spot**
When a CLI tool has a `requiredUnless("deprecated-flag")` constraint, the deprecated flag remains implicitly supported forever. Search for `requiredUnless` and `requiredIf` in option parsers to find deprecated alternatives that should be removed during a major version bump.

**Test**
`testConfigBrokerList` and `testBrokerListOverride` tests removed; updated remaining tests to use `--bootstrap-server`.
