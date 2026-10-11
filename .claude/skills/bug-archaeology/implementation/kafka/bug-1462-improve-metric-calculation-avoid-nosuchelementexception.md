**Commit**: 852f14065b · **Ticket**: KAFKA-18689
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Nick Telford <nick.telford@gmail.com>, Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
import java.util.NoSuchElementException;
                    try {
                        return openIterators.isEmpty() ? null : openIterators.first().startTimestamp();
                    } catch (final NoSuchElementException ignored) {
                        return null;
                    }
import java.util.NoSuchElementException;
                    try {
                        return openIterators.isEmpty() ? null : openIterators.first().startTimestamp();
                    } catch (final NoSuchElementException ignored) {
                        return null;
                    }

// After
import java.util.Iterator;
                    final Iterator<MeteredIterator> openIteratorsIterator = openIterators.iterator();
                    return openIteratorsIterator.hasNext() ? openIteratorsIterator.next().startTimestamp() : null;
import java.util.Iterator;
                    final Iterator<MeteredIterator> openIteratorsIterator = openIterators.iterator();
                    return openIteratorsIterator.hasNext() ? openIteratorsIterator.next().startTimestamp() : null;
import java.util.Iterator;
                    final Iterator<MeteredIterator> openIteratorsIterator = openIterators.iterator();
                    return openIteratorsIterator.hasNext() ? openIteratorsIterator.next().startTimestamp() : null;
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18689: improve metric calculation to avoid nosuchelementexception (#18771).

**Test**
No test added.
