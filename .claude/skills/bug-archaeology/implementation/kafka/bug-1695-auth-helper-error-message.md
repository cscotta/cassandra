## Commit

- Hash: 4b1b67e3c4
- Tickets: KAFKA-18434
- Subject: KAFKA-18434: Improve authorization failure error message to include required operation

## Subsystem

core / AuthHelper

## Category

Logic error in condition

## Root cause

When a request was rejected due to missing ACLs, `AuthHelper.scala` emitted the message `"Request $request is not authorized."` This message told the operator what was rejected but not which ACL operation was missing, forcing them to cross-reference the Kafka authorization documentation to figure out what permission to grant. The new message names the required operation directly.

## Before

```scala
s"Request $request is not authorized."
```

## After

```scala
s"Request $request needs $operation permission."
```

## How to spot

Authorization-failure log/error messages that mention the denied request or resource but omit the required ACL operation type — operators need the operation name to write the correct `kafka-acls` command.

## Test

`AuthHelperTest` updated to assert the new message format including the operation name.
