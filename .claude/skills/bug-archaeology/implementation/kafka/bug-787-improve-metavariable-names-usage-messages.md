**Commit**: 4b9075b506 · **Ticket**: KAFKA-19653
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
This trivial PR improves the so-called metavariable names in the usage
messages of the verifiable producer/consumer command-line tools. These
are the names of the replacement variables that appear solely in the
usage messages. Verifiable producer (before):
```
usage: verifiable-producer [-h] --topic TOPIC
    [--max-messages MAX-MESSAGES] [--throughput THROUGHPUT]
    [--acks ACKS] [--producer.config CONFIG_FILE]
    [--message-create-time CREATETIME] [--value-prefix VALUE-PREFIX]
    [--repeating-keys REPEATING-KEYS] [--command-config CONFIG_FILE]
    --bootstrap-server HOST1:PORT1[,HOST2:PORT2[...]]
```

(after)
```
usage: verifiable-producer [-h] --topic TOPIC
    [--max-messages MAX-MESS...

**Before/After**
```java
// Before
                .newArgumentParser("verifiable-consumer")
                .defaultHelp(true)
                .description("This tool consumes messages from a specific topic and emits consumer events (e.g. group rebalances, received messages, and offsets committed) as JSON objects to STDOUT.");
                .description("Group of arguments for connection to brokers")
                .required(true);
                .action(store())
                .required(true)
                .type(String.class)
                .metavar("HOST1:PORT1[,HOST2:PORT2[...]]")
                .dest("bootstrapServer")
                .help("The server(s) to connect to. Comma-separated list of Kafka brokers in the form HOST1:PORT1,HOST2:PORT2,...");
                .action(store())

// After
            .newArgumentParser("verifiable-consumer")
            .defaultHelp(true)
            .description("This tool consumes messages from a specific topic and emits consumer events (e.g. group rebalances, received messages, and offsets committed) as JSON objects to STDOUT.");
            .description("Group of arguments for connection to brokers")
            .required(true);
            .action(store())
            .required(true)
            .type(String.class)
            .dest("bootstrapServer")
            .metavar("HOST1:PORT1[,HOST2:PORT2[...]]")
            .help("The server(s) to connect to. Comma-separated list of Kafka brokers in the form HOST1:PORT1,HOST2:PORT2,...");
            .action(store())
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19653: improve metavariable names in usage messages (#20438).

**Test**
No test added.
