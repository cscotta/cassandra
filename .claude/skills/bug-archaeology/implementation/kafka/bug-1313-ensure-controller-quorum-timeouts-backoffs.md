**Commit**: a2653281c2 · **Ticket**: KAFKA-18837
**Subsystem**: raft/src
**Category**: Logic error in condition

**Root cause**
Add validators to the QuorumConfig's configuration definitions. Reviewers: Mickael Maison <mickael.maison@gmail.com>, Ken Huang <s7133700@gmail.com>, TengYao Chi <kitingiao@gmail.com>

**Before/After**
```java
// Before
            .define(QUORUM_ELECTION_TIMEOUT_MS_CONFIG, INT, DEFAULT_QUORUM_ELECTION_TIMEOUT_MS, null, HIGH, QUORUM_ELECTION_TIMEOUT_MS_DOC)
            .define(QUORUM_FETCH_TIMEOUT_MS_CONFIG, INT, DEFAULT_QUORUM_FETCH_TIMEOUT_MS, null, HIGH, QUORUM_FETCH_TIMEOUT_MS_DOC)
            .define(QUORUM_ELECTION_BACKOFF_MAX_MS_CONFIG, INT, DEFAULT_QUORUM_ELECTION_BACKOFF_MAX_MS, null, HIGH, QUORUM_ELECTION_BACKOFF_MAX_MS_DOC)
            .define(QUORUM_LINGER_MS_CONFIG, INT, DEFAULT_QUORUM_LINGER_MS, null, MEDIUM, QUORUM_LINGER_MS_DOC)
            .define(QUORUM_REQUEST_TIMEOUT_MS_CONFIG, INT, DEFAULT_QUORUM_REQUEST_TIMEOUT_MS, null, MEDIUM, QUORUM_REQUEST_TIMEOUT_MS_DOC)
            .define(QUORUM_RETRY_BACKOFF_MS_CONFIG, INT, DEFAULT_QUORUM_RETRY_BACKOFF_MS, null, LOW, QUORUM_RETRY_BACKOFF_MS_DOC);

// After
import static org.apache.kafka.common.config.ConfigDef.Range.atLeast;
            .define(QUORUM_ELECTION_TIMEOUT_MS_CONFIG, INT, DEFAULT_QUORUM_ELECTION_TIMEOUT_MS, atLeast(0), HIGH, QUORUM_ELECTION_TIMEOUT_MS_DOC)
            .define(QUORUM_FETCH_TIMEOUT_MS_CONFIG, INT, DEFAULT_QUORUM_FETCH_TIMEOUT_MS, atLeast(0), HIGH, QUORUM_FETCH_TIMEOUT_MS_DOC)
            .define(QUORUM_ELECTION_BACKOFF_MAX_MS_CONFIG, INT, DEFAULT_QUORUM_ELECTION_BACKOFF_MAX_MS, atLeast(0), HIGH, QUORUM_ELECTION_BACKOFF_MAX_MS_DOC)
            .define(QUORUM_LINGER_MS_CONFIG, INT, DEFAULT_QUORUM_LINGER_MS, atLeast(0), MEDIUM, QUORUM_LINGER_MS_DOC)
            .define(QUORUM_REQUEST_TIMEOUT_MS_CONFIG, INT, DEFAULT_QUORUM_REQUEST_TIMEOUT_MS, atLeast(0), MEDIUM, QUORUM_REQUEST_TIMEOUT_MS_DOC)
            .define(QUORUM_RETRY_BACKOFF_MS_CONFIG, INT, DEFAULT_QUORUM_RETRY_BACKOFF_MS, atLeast(0), LOW, QUORUM_RETRY_BACKOFF_MS_DOC);
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
```

**How to spot**
When reviewing raft/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18837: ensure controller quorum timeouts and backoffs are at least 0 (#18998).

**Test**
Test modified in QuorumConfigTest.java covering the corrected behaviour.
