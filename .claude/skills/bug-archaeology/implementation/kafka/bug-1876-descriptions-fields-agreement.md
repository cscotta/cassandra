**Commit**: 38e727fe4d · **Ticket**: KAFKA-17864
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Improve descriptive information in Kafka protocol documentation. Reviewers: Mickael Maison <mickael.maison@gmail.com>, Andrew Schofield <aschofield@confluent.io>, Apoorv Mittal <apoorvmittal10@gmail.com>

**Before/After**
```java
// Before
        "about": "The partition indexes to add to the transaction" }
        "about": "The results for each partition" }
    { "name": "ClusterId", "type": "string", "versions": "0+", "nullableVersions": "0+" },
    { "name": "TimeoutMs", "type": "int32", "versions": "0+" },
      "about": "The replica id of the voter getting added to the topic partition" },
      "about": "The directory id of the voter getting added to the topic partition" },
      "about": "The endpoints that can be used to communicate with the voter", "fields": [
        "about": "The name of the endpoint" },
        "about": "The hostname" },
        "about": "The port" }
      "about": "The error code, or 0 if there was no error" },
      "about": "The ID of the requesting broker" },

// After
        "about": "The partition indexes to add to the transaction." }
        "about": "The results for each partition." }
    { "name": "ClusterId", "type": "string", "versions": "0+", "nullableVersions": "0+",
      "about": "The cluster id." },
    { "name": "TimeoutMs", "type": "int32", "versions": "0+",
      "about": "The maximum time to wait for the request to complete before returning."},
      "about": "The replica id of the voter getting added to the topic partition." },
      "about": "The directory id of the voter getting added to the topic partition." },
      "about": "The endpoints that can be used to communicate with the voter.", "fields": [
        "about": "The name of the endpoint." },
        "about": "The hostname." },
        "about": "The port." }
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17864: add descriptions to fields in the agreement (#17681).

**Test**
No test added.
