**Commit**: be6feb2f63 · **Ticket**: KAFKA-20354
**Subsystem**: docs/implementation
**Category**: Logic error in condition

**Root cause**
This patch adds missing Control Batch types in the documentation. Reviewers: Luke Chen <showuon@gmail.com>, Andrew Schofield <aschofield@apache.org>, Mickael Maison <mickael.maison@gmail.com>

---------

Signed-off-by: Federico Valeri <fedevaleri@gmail.com>

**Before/After**
```java
// Before
A control batch contains a single record called the control record. Control records should not be passed on to applications. Instead, they are used by consumers to filter out aborted transactional messages.
The key of a control record conforms to the following schema:
type: int16 (0 indicates an abort marker, 1 indicates a commit)
The schema for the value of a control record is dependent on the type. The value is opaque to clients.

// After
A control batch contains a single record called the control record. Control records should not be returned to applications. Instead, they are used by consumers to filter out aborted transactional messages, and by the KRaft implementation for its protocol metadata.
The key of a control record conforms to the following schema:
type: int16 (the control record types are in the table below)
The following control record types are currently defined for regular topics:

<table>
<tr>
<th>

Type
</th>
<th>
```

**How to spot**
When reviewing docs/implementation code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20354: add missing control batch types in documentation (#21866).

**Test**
No test added.
