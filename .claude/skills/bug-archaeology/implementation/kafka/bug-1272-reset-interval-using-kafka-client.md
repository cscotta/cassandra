**Commit**: b7728d904d · **Ticket**: KAFKA-18984
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
kafka-client-metrics.sh cannot reset the interval using `--interval=`. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
            opts.interval().map(intervalVal -> configsToBeSet.put("interval.ms", intervalVal.toString()));
        private final ArgumentAcceptingOptionSpec<Integer> intervalOpt;
            intervalOpt = parser.accepts("interval", "The metrics push interval in milliseconds.")
                .ofType(java.lang.Integer.class);
            String nl = System.lineSeparator();
            matchOpt = parser.accepts("match",  "Matching selector 'k1=v1,k2=v2'. The following is a list of valid selector names: " + nl + matchSelectorNames)
            metricsOpt = parser.accepts("metrics",  "Telemetry metric name prefixes 'm1,m2'.")
        public Optional<Integer> interval() {

// After
            opts.interval().map(intervalVal -> configsToBeSet.put("interval.ms", intervalVal));
        private final ArgumentAcceptingOptionSpec<String> intervalOpt;
            String nl = System.lineSeparator();

            intervalOpt = parser.accepts("interval", "The metrics push interval in milliseconds." + nl + "Leave empty to reset the interval.")
                .ofType(String.class);
            matchOpt = parser.accepts("match", "Matching selector 'k1=v1,k2=v2'. The following is a list of valid selector names: " + nl + matchSelectorNames)
            metricsOpt = parser.accepts("metrics", "Telemetry metric name prefixes 'm1,m2'.")
        public Optional<String> interval() {

                interval().ifPresent(intervalStr -> {
                    if (!intervalStr.isEmpty()) {
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18984: reset interval.ms by using kafka-client-metrics.sh (#19213).

**Test**
Test modified in ClientMetricsCommandTest.java covering the corrected behaviour.
