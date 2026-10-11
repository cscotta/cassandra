**Commit**: 09713a38fb · **Ticket**: KAFKA-19749
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
# Description
In kafka-features.sh, exclusive argument group of `--bootstrap-server`
and `--bootstrap-controller` is required in the main parser, but
`version-mapping` and `feature-dependencies` subcommands don't need the
information. # Changes
To avoid change in argument level, the exclusive group is not moved. It
is now not required and only check for existence if the subcommand needs
it. Since `FeatureCommand#mainNoExit` catches `ArgumentParserException`,
`ArgumentParser#parseArgs` is used instead of `parseArgsOrFail`.

**Before/After**
```java
// Before
        MutuallyExclusiveGroup bootstrapGroup = parser.addMutuallyExclusiveGroup().required(true);
        Namespace namespace = parser.parseArgsOrFail(args);
        CommandLineUtils.initializeBootstrapProperties(properties,
            Optional.ofNullable(namespace.getString("bootstrap_server")),
            Optional.ofNullable(namespace.getString("bootstrap_controller")));
                case "version-mapping":
                    handleVersionMapping(namespace, Feature.PRODUCTION_FEATURES);
                    break;
                case "feature-dependencies":
                    handleFeatureDependencies(namespace, Feature.PRODUCTION_FEATURES);
                    break;

// After
        MutuallyExclusiveGroup bootstrapGroup = parser.addMutuallyExclusiveGroup();
        Namespace namespace = parser.parseArgs(args);
        if (command.equals("version-mapping")) {
            handleVersionMapping(namespace, Feature.PRODUCTION_FEATURES);
            return;
        } else if (command.equals("feature-dependencies")) {
            handleFeatureDependencies(namespace, Feature.PRODUCTION_FEATURES);
            return;
        }
        try {
            CommandLineUtils.initializeBootstrapProperties(properties,
                    Optional.ofNullable(namespace.getString("bootstrap_server")),
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19749 the `version-mapping` of kafka-features.sh should work without requiring the bootstrap (#20619).

**Test**
Test modified in FeatureCommandTest.java covering the corrected behaviour.
