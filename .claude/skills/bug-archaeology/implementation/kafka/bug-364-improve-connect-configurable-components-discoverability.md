**Commit**: f35c72c5de · **Ticket**: KAFKA-20079
**Subsystem**: connect/api
**Category**: Logic error in condition

**Root cause**
This implements [KIP-1273](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1273%3A+Improve+Connect+configurable+components+discoverability)

Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
 * Provides common interfaces used to describe pluggable components.
import org.apache.kafka.connect.components.Versioned;
public abstract class Connector implements Versioned {
public interface ConnectorClientConfigOverridePolicy extends Configurable, AutoCloseable {

// After
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
```

**How to spot**
When reviewing connect/api code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20079: improve connect configurable components discoverability (#21330).

**Test**
Test modified in ConnectorValidationIntegrationTest.java covering the corrected behaviour.
