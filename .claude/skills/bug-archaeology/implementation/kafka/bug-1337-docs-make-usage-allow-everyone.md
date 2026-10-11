**Commit**: a9bc7c5889 · **Ticket**: KAFKA-18766
**Subsystem**: docs/security.html
**Category**: Logic error in condition

**Root cause**
In the documentation today, we have the following sentence:

By default, if no ResourcePatterns match a specific Resource R, then R
has no associated ACLs, and therefore no one other than super users is
allowed to access R. If you want to change that behavior, you can
include the following in server.properties. Reviewers: TengYao Chi <kitingiao@gmail.com>, Andrew Schofield <aschofield@confluent.io>
 

Although, it is correct, I have observed users being confused by it. I
think could me made clearer that default is deny and this property is a
way to change default.

**Before/After**
```java
// Before
    In order to add, remove, or list ACLs, you can use the Kafka ACL CLI <code>kafka-acls.sh</code>. By default, if no ResourcePatterns match a specific Resource R,
    then R has no associated ACLs, and therefore no one other than super users is allowed to access R.
    If you want to change that behavior, you can include the following in server.properties.
    One can also add super users in server.properties like the following (note that the delimiter is semicolon since SSL user names may contain comma). Default PrincipalType string "User" is case sensitive.

// After
    In order to add, remove, or list ACLs, you can use the Kafka ACL CLI <code>kafka-acls.sh</code>.
    <h5><u>Behavior Without ACLs:</u></h5>
    <p>If a resource (R) does not have any ACLs defined, meaning that no ACL matches the resource, Kafka will restrict
        access to that resource. In this situation, only super users are allowed to access it.</p>
    <h5><u>Changing the Default Behavior:</u></h5>
    <p>If you prefer that resources without any ACLs be accessible by all users (instead of just super users), you can
        change the default behavior. To do this, add the following line to your server.properties file:</p>
    <p>With this setting enabled, if a resource does not have any ACLs defined, Kafka will allow access to everyone. If a
        resource has one or more ACLs defined, those ACL rules will be enforced as usual, regardless of the setting.
        One can also add super users in server.properties like the following (note that the delimiter is semicolon since SSL
        user names may contain comma). Default PrincipalType string "User" is case sensitive.
    </p>
```

**How to spot**
When reviewing docs/security.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18766:docs: make usage of allow.everyone.if.no.acl.found config clearer (#19077).

**Test**
No test added.
