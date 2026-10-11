**Commit**: 5a2b03b200 · **Ticket**: KAFKA-18235
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Christopher L. Shannon <christopher.l.shannon@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    swagger libs.swaggerJaxrs2
  swaggerJaxrs2: "io.swagger.core.v3:swagger-jaxrs2:$swaggerVersion",

// After
    swagger libs.jakartaServletApi
    swagger libs.jaxrs2Jakarta
  jakartaServletApi: "6.1.0",
  jakartaServletApi: "jakarta.servlet:jakarta.servlet-api:$versions.jakartaServletApi",
  jaxrs2Jakarta: "io.swagger.core.v3:swagger-jaxrs2-jakarta:$swaggerVersion",
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18235 can't build release tar due to broken genconnectopenapidocs task (#18177).

**Test**
No test added.
