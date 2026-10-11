**Commit**: f746d67c3b · **Ticket**: KAFKA-16373
**Subsystem**: docker/docker_official_images
**Category**: Logic error in condition

**Root cause**
This PR aims to add the static Dockerfile and scripts for AK 3.7.0 version. As mentioned in KIP-1028 this PR aims to start the release of the kafka:3.7.0 Docker Official image. This will also help us validate the process and allow us to address any changes suggested by Dockerhub before the 3.8.0 release. The static Dockerfile and scripts have been generated via the github actions workflows and scripts added as part of https://github.com/apache/kafka/pull/16027.

**Before/After**
```java
// Before
// (no removals)

// After
###############################################################################
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
```

**How to spot**
When reviewing docker/docker_official_images code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16373: kip-1028: adding 3.7.0 docker official images static assets (#16267).

**Test**
No test added.
