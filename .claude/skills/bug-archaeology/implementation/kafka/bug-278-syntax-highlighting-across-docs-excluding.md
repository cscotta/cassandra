**Commit**: 7303c1e0e0 · **Ticket**: KAFKA-20366
**Subsystem**: docs/apis
**Category**: Logic error in condition

**Root cause**
Add syntax highlighting across docs, excluding security and streams. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before


    <dependency>
    	<groupId>org.apache.kafka</groupId>
    	<artifactId>kafka-clients</artifactId>
    	<version>4.3.0</version>
    </dependency>


    <dependency>
    	<groupId>org.apache.kafka</groupId>
    	<artifactId>kafka-clients</artifactId>

// After

```xml
<dependency>
	<groupId>org.apache.kafka</groupId>
	<artifactId>kafka-clients</artifactId>
	<version>4.3.0</version>
</dependency>
```

```xml
<dependency>
	<groupId>org.apache.kafka</groupId>
```

**How to spot**
When reviewing docs/apis code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20366: add syntax highlighting across docs, excluding security and streams (#21887).

**Test**
No test added.
