**Commit**: 203f3237ec · **Ticket**: KAFKA-17374
**Subsystem**: tools (ReassignPartitionsCommand)
**Category**: Logic error in condition

**Root cause**
`ReassignPartitionsCommand` declared `--bootstrap-server` as a required option, which blocked controller-only workflows in KRaft mode where no broker is available. Users who only had controller endpoints could not use the tool at all. The fix adds a mutually exclusive `--bootstrap-controller` option and routes it through `BOOTSTRAP_CONTROLLERS_CONFIG`, mirroring the pattern already established by `AclCommand`.

**Before/After**
```java
// Before – bootstrap-server required unconditionally
opts.bootstrapServerOpt = parser.accepts("bootstrap-server")
    .withRequiredArg().required();

// After – either bootstrap-server OR bootstrap-controller
if (!opts.has(bootstrapServerOpt) && !opts.has(bootstrapControllerOpt))
    CommandLineUtils.printUsageAndExit(parser,
        "One of --bootstrap-server or --bootstrap-controller must be specified");
```

**How to spot**
Any admin tool that hard-requires `--bootstrap-server` is broken for KRaft controller-only deployments. Audit each CLI tool for `required()` on the bootstrap option and replace with a mutual-exclusion check that also accepts `--bootstrap-controller`.

**Test**
Unit tests added in `ReassignPartitionsCommandTest` for the controller path.
