**Commit**: 18340c9733 · **Ticket**: KAFKA-17563
**Subsystem**: checkstyle/import-control-server.xml
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@apache.org>

**Before/After**
```java
// Before
        val desc = RequestConvertToJson.requestDescMetrics(header, requestLog, response.responseLog,
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

// After
  <subpackage name="network">
    <allow pkg="com.fasterxml.jackson" />
  </subpackage>

    <suppress checks="ClassFanOutComplexity|CyclomaticComplexity|MethodLength|ParameterNumber|JavaNCSS|ImportControl" files="RequestConvertToJson.java"/>
import org.apache.kafka.network.RequestConvertToJson
import scala.compat.java8.OptionConverters._
        val desc = RequestConvertToJson.requestDescMetrics(header, requestLog.asJava, response.responseLog.asJava,
```

**How to spot**
When reviewing checkstyle/import-control-server.xml code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17563 move `requestconverttojson` to server module (#17223).

**Test**
Test modified in RequestChannelTest.scala covering the corrected behaviour.
