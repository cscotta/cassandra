**Commit**: b7e69ebf5c · **Ticket**: KAFKA-17924
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
These metrics were deprecated in KIP-773 and are being removed in Kafka 4.0. Reviewers:  Chia-Ping Tsai <chia7712@gmail.com>, Divij Vaidya <diviv@amazon.com>

**Before/After**
```java
// Before
        MetricName totalMetricName = metrics.metricName("bufferpool-wait-time-total",
                                                   metricGrpName,
                                                   "*Deprecated* The total time an appender waits for space allocation.");
        this.waitTime.add(new Meter(TimeUnit.NANOSECONDS, rateMetricName, totalMetricName));
            this.selectTime.add(createIOThreadRatioMeterLegacy(metrics, metricGrpName, metricTags, "io-wait", "waiting"));
            this.ioTime.add(createIOThreadRatioMeterLegacy(metrics, metricGrpName, metricTags, "io", "doing I/O"));
        /**
         * This method generates `time-total` metrics but has a couple of deficiencies: no `-ns` suffix and no dash between basename
         * and `time-total` suffix.
         * @deprecated use {{@link #createIOThreadRatioMeter(Metrics, String, Map, String, String)}} for new metrics instead
         */
        @Deprecated

// After
// (no additions)
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17924: remove `bufferpool-wait-time-total`, `io-waittime-total`, and `iotime-total` (#17697).

**Test**
Test modified in SelectorTest.java covering the corrected behaviour.
