**Commit**: b3ba7bc929 · **Ticket**: KAFKA-18782
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
**Summary**
Extend ApplicationRecoverableException related exceptions

Reviewers: Artem Livshits <alivshits@confluent.io>, Justine Olshan
 <jolshan@confluent.io>

**Before/After**
```java
// Before
public class FencedInstanceIdException extends ApiException {
public class IllegalGenerationException extends ApiException {
public class InvalidPidMappingException extends ApiException {
public class InvalidProducerEpochException extends ApiException {
public class ProducerFencedException extends ApiException {
public class UnknownMemberIdException extends ApiException {

// After
    private static final long serialVersionUID = 1L;

    public ApplicationRecoverableException(String message, Throwable cause) {
        super(message, cause);
    }


    public ApplicationRecoverableException(Throwable cause) {
        super(cause);
    }

    public ApplicationRecoverableException() {
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18782: extend applicationrecoverableexception related exceptions (#19354).

**Test**
Test modified in TransactionExceptionHierarchyTest.java covering the corrected behaviour.
