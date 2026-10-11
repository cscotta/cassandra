**Commit**: 6551e87815 · **Ticket**: KAFKA-18925
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Add support so that Admin.listGroups can represent
streams groups and their states. Reviewers: Bill Bejeck <bill@confluent.io>

**Before/After**
```java
// Before
 *         <tr><th>State</th><th>Classic group</th><th>Consumer group</th><th>Share group</th></tr>
 *         <tr><td>UNKNOWN</td><td>Yes</td><td>Yes</td><td>Yes</td></tr>
 *         <tr><td>PREPARING_REBALANCE</td><td>Yes</td><td>Yes</td><td></td></tr>
 *         <tr><td>COMPLETING_REBALANCE</td><td>Yes</td><td>Yes</td><td></td></tr>
 *         <tr><td>STABLE</td><td>Yes</td><td>Yes</td><td>Yes</td></tr>
 *         <tr><td>DEAD</td><td>Yes</td><td>Yes</td><td>Yes</td></tr>
 *         <tr><td>EMPTY</td><td>Yes</td><td>Yes</td><td>Yes</td></tr>
 *         <tr><td>ASSIGNING</td><td></td><td>Yes</td><td></td></tr>
 *         <tr><td>RECONCILING</td><td></td><td>Yes</td><td></td></tr>
    RECONCILING("Reconciling");
    SHARE("Share");

// After
 *         <tr><th>State</th><th>Classic group</th><th>Consumer group</th><th>Share group</th><th>Streams group</th></tr>
 *         <tr><td>UNKNOWN</td><td>Yes</td><td>Yes</td><td>Yes</td><td>Yes</td></tr>
 *         <tr><td>PREPARING_REBALANCE</td><td>Yes</td><td>Yes</td><td></td><td></td></tr>
 *         <tr><td>COMPLETING_REBALANCE</td><td>Yes</td><td>Yes</td><td></td><td></td></tr>
 *         <tr><td>STABLE</td><td>Yes</td><td>Yes</td><td>Yes</td><td>Yes</td></tr>
 *         <tr><td>DEAD</td><td>Yes</td><td>Yes</td><td>Yes</td><td>Yes</td></tr>
 *         <tr><td>EMPTY</td><td>Yes</td><td>Yes</td><td>Yes</td><td>Yes</td></tr>
 *         <tr><td>ASSIGNING</td><td></td><td>Yes</td><td></td><td>Yes</td></tr>
 *         <tr><td>RECONCILING</td><td></td><td>Yes</td><td></td><td>Yes</td></tr>
 *         <tr><td>NOT_READY</td><td></td><td></td><td></td><td>Yes</td></tr>
    RECONCILING("Reconciling"),
    NOT_READY("NotReady");
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18925: add streams groups support to admin.listgroups (#19155).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
