**Commit**: 65f789f560 · **Ticket**: KAFKA-19626
**Subsystem**: docs/streams
**Category**: Logic error in condition

**Root cause**
This implements [KIP-1147](https://cwiki.apache.org/confluence/x/DguWF)
for kafka-cluster.sh, kafka-leader-election.sh and
kafka-streams-application-reset.sh. Jira: https://issues.apache.org/jira/browse/KAFKA-19626

Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
                                        passed to admin clients and embedded
                                        consumer.
            subpparser.addArgument("--config", "-c")
                    .help("A property file containing configurations for the Admin client.");
        String configPath = namespace.getString("config");
        Properties properties = (configPath == null) ? new Properties() : Utils.loadProps(configPath);
        Properties props = new Properties();
            props.putAll(Utils.loadProps(commandOptions.getAdminClientConfig()));

// After
--config-file &lt;String: file name&gt;    (Deprecated) Property file containing configs to be
                                        passed to admin clients and embedded consumer.
                                        This option will be removed in a future version.
                                        Use --command-config instead.
--command-config &lt;String: file name&gt; Config properties file to be passed to admin clients
                                        and embedded consumer.
import net.sourceforge.argparse4j.inf.ArgumentParserException;
            subpparser.addArgument("--config")
                    .help("(DEPRECATED) A property file containing configurations for the Admin client. " +
                            "This option will be removed in a future version. Use --command-config instead.");
            subpparser.addArgument("--command-config", "-c")
                    .action(store())
```

**How to spot**
When reviewing docs/streams code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19626: kip-1147 consistency of command-line arguments for remaining cli tools (#20431).

**Test**
Test modified in ClusterToolTest.java covering the corrected behaviour.
