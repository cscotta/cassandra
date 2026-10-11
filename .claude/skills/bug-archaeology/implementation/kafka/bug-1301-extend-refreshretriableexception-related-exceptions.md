**Commit**: c32c167e04 · **Ticket**: KAFKA-18781
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
- Extended derived exceptions described in
[KIP-1050](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=309496816#KIP1050:ConsistenterrorhandlingforTransactions-RefreshRetriableException)
to include the new RefreshRetriableException in base hierarchy. - Added unit tests to validate the hierarchy of the derived exceptions
in relevant scenarios. Reviewers: Justine Olshan <jolshan@confluent.io>

**Before/After**
```java
// Before
public class CoordinatorNotAvailableException extends RetriableException {
public abstract class InvalidMetadataException extends RetriableException {
public class NotCoordinatorException extends RetriableException {

// After
public class CoordinatorNotAvailableException extends RefreshRetriableException {
public abstract class InvalidMetadataException extends RefreshRetriableException {
public class NotCoordinatorException extends RefreshRetriableException {
    private static final long serialVersionUID = 1L;

    public RefreshRetriableException(String message, Throwable cause) {
        super(message, cause);
    }


    public RefreshRetriableException(Throwable cause) {
        super(cause);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18781: extend refreshretriableexception related exceptions (#19136).

**Test**
Test modified in TransactionExceptionHierarchyTest.java covering the corrected behaviour.
