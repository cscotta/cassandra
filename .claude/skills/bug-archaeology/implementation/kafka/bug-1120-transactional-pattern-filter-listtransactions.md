**Commit**: 0c1fbf3aeb · **Ticket**: KAFKA-19073
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Propose adding a new filter TransactionalIdPattern. This transaction ID pattern filter works as AND with the other transaction filters. Also, it is empowered with Re2j. KIP: https://cwiki.apache.org/confluence/x/4gm9F

Reviewers: Justine Olshan <jolshan@confluent.io>, Ken Huang
<s7133700@gmail.com>, Kuan-Po Tseng <brandboat@gmail.com>, Chia-Ping
Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            Objects.equals(filteredDuration, that.filteredDuration);
        return Objects.hash(filteredStates, filteredProducerIds, filteredDuration);

// After
    implementation libs.re2j
    private String filteredTransactionalIdPattern;
    /**
     * Filter only the transactions that match with the given transactional ID pattern.
     * If the filter is null or if the passed string is empty,
     * then all the transactions will be returned.
     *
     * @param pattern the transactional ID regular expression pattern to filter by
     * @return this object
     */
    public ListTransactionsOptions filterOnTransactionalIdPattern(String pattern) {
        this.filteredTransactionalIdPattern = pattern;
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19073 add transactional id pattern filter to listtransactions (#19355).

**Test**
Test modified in ListTransactionsHandlerTest.java covering the corrected behaviour.
