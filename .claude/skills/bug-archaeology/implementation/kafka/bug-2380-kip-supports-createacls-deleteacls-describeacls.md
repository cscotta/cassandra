**Commit**: 4692aeb671 · **Ticket**: KAFKA-17036
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            new LeastLoadedNodeProvider()) {
            new LeastLoadedNodeProvider()) {
            new LeastLoadedNodeProvider()) {
    "--bootstrap-server instead to set ACLs through the admin client."
      if (opts.options.has(opts.bootstrapServerOpt)) {
      props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, opts.options.valueOf(opts.bootstrapServerOpt))
      if (options.has(bootstrapServerOpt) && options.has(authorizerOpt))
        CommandLineUtils.printUsageAndExit(parser, "Only one of --bootstrap-server or --authorizer must be specified")
      if (!options.has(bootstrapServerOpt)) {
      if (options.has(commandConfigOpt) && !options.has(bootstrapServerOpt))
        CommandLineUtils.printUsageAndExit(parser, "The --command-config o

// After
            new LeastLoadedBrokerOrActiveKController()) {
            new LeastLoadedBrokerOrActiveKController()) {
            new LeastLoadedBrokerOrActiveKController()) {
    "--bootstrap-server or --bootstrap-controller instead to set ACLs through the admin client."
      if (opts.options.has(opts.bootstrapServerOpt) || opts.options.has(opts.bootstrapControllerOpt)) {

      if (opts.options.has(opts.bootstrapServerOpt)) {
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, opts.options.valueOf(opts.bootstrapServerOpt))
      } else {
        props.put(AdminClientConfig.BOOTSTRAP_CONTROLLERS_CONFIG, opts.options.valueOf(opts.bootstrapControllerOpt))
      }
    val bootstrapControllerOpt: OptionSpec[String] = parser.accepts("bootstrap-controller", "A list of host/port pairs to use for establishing the connection to the Kafka cluster." +
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17036 kip-919 supports for createacls, deleteacls, describeacls (#16493).

**Test**
Test modified in AclCommandTest.java covering the corrected behaviour.
