**Commit**: 4fedffd282 · **Ticket**: KAFKA-19429
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
It looks like we are checking for properties that are not guaranteed
under at_least_once, for example, exact counting (not allowing for
overcounting). This change relaxes the validation constraint to only check that we
counted _at least_ N messages, and our sums come out as _at least_ the
expected sum. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
            pass = verifyTAgg(resultStream, inputs, events.get("tagg"), printResults);
            }, printResults, eosEnabled);
            pass &= verify(resultStream, "min", inputs, events, SmokeTestDriver::getMin, printResults, eosEnabled);
            pass &= verify(resultStream, "max", inputs, events, SmokeTestDriver::getMax, printResults, eosEnabled);
            pass &= verify(resultStream, "dif", inputs, events, key -> getMax(key).intValue() - getMin(key).intValue(), printResults, eosEnabled);
            pass &= verify(resultStream, "sum", inputs, events, SmokeTestDriver::getSum, printResults, eosEnabled);
            pass &= verify(resultStream, "cnt", inputs, events, key1 -> getMax(key1).intValue() - getMin(key1).intValue() + 1L, printResults, eosEnabled);
            pass &= verify(resultStream, "avg", inputs, events, SmokeTestDriver::getAvg, printResults, eosEnabled);

                if (!expected.equals(actual)) {

// After
import java.util.Objects;
import java.util.function.BiPredicate;
        final BiPredicate<Number, Number> validationPredicate;
        if (eosEnabled) {
            validationPredicate = Objects::equals;
        } else {
            validationPredicate = SmokeTestDriver::lessEquals;
        }
            pass = verifyTAgg(resultStream, inputs, events.get("tagg"), validationPredicate, printResults);
            }, Object::equals, printResults, eosEnabled);
            pass &= verify(resultStream, "min", inputs, events, SmokeTestDriver::getMin, Object::equals, printResults, eosEnabled);
            pass &= verify(resultStream, "max", inputs, events, SmokeTestDriver::getMax, Object::equals, printResults, eosEnabled);
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19429: deflake streams_smoke_test (#20019).

**Test**
Test modified in SmokeTestDriver.java covering the corrected behaviour.
