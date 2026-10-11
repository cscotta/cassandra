**Commit**: 2ae85ef214 · **Ticket**: KAFKA-19429
**Subsystem**: streams/src
**Category**: Missing null / bounds check

**Root cause**
It looks like we are checking for properties that are not guaranteed
under at_least_once, for example, exact counting (not allowing for
overcounting). This change relaxes the validation constraint:

The TAGG topic contains effectively count-by-count results. So for
example, if we have the input without duplication

0 -> 1,2,3 we will get in TAGG 3 -> 1, since 1 key had 3 values. with duplication:

0 -> 1,1,2,3 we will get in TAGG 4 -> 1, since 1 key had 4 values.

**Before/After**
```java
// Before
            pass = verifyTAgg(resultStream, inputs, events.get("tagg"), validationPredicate, printResults);
            pass &= verify(resultStream, "avg", inputs, events, SmokeTestDriver::getAvg, validationPredicate, printResults, eosEnabled);
                            indent("\t\t", observedInputEvents.get(key)),
                                      final BiPredicate<Number, Number> validationPredicate,
                if (!validationPredicate.test(expectedCount, entry.getValue().getLast().value())) {

// After
            pass = true;
            if (eosEnabled) {
                // TAGG is computing "Count-by-count", which may produce keys that are not in the input data in ALOS, so we skip validation in this case.
                pass = verifyTAgg(resultStream, inputs, events.get("tagg"), printResults);
            }
            if (eosEnabled) {
                // Average can overcount and undercount in ALOS, so we skip validation in that case.
                pass &= verify(resultStream, "avg", inputs, events, SmokeTestDriver::getAvg, Object::equals, printResults, eosEnabled);
            }
                            indent("\t\t", observedInputEvents.getOrDefault(key, new LinkedList<>())),
                if (entry.getValue().getLast().value().longValue() != expectedCount) {
```

**How to spot**
When reviewing streams/src code, look for missing null / bounds check patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19429: deflake streams_smoke_test, again (#20070).

**Test**
Test modified in SmokeTestDriver.java covering the corrected behaviour.
