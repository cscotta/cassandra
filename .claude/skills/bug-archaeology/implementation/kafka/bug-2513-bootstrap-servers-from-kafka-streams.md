**Commit**: c7f0ade7e0 · **Ticket**: KAFKA-16334
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Matthias J. Sax <matthias@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            } else if (options.hasBootstrapServers()) {
                bootstrapServerValue = options.bootstrapServers();
        private final OptionSpec<String> bootstrapServersOption;
            bootstrapServersOption = parser.accepts("bootstrap-servers", "DEPRECATED: Comma-separated list of broker urls with format: HOST1:PORT1,HOST2:PORT2")
                .withRequiredArg()
                .ofType(String.class)
                .describedAs("urls");
            bootstrapServerOption = parser.accepts("bootstrap-server", "REQUIRED unless --bootstrap-servers(deprecated) is specified. The server(s) to connect to. The broker list string in the form HOST1:PORT1,HOST2:PORT2. (default: localhost:9092)")
        public boolean hasBootstrapServers() {
            return options.has(bootstrapServersOption);
        }


// After
            bootstrapServerOption = parser.accepts("bootstrap-server", "The server(s) to connect to. The broker list string in the form HOST1:PORT1,HOST2:PORT2. (default: localhost:9092)")
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16334: remove --bootstrap-servers from kafka-streams-application-reset.

**Test**
No test added.
