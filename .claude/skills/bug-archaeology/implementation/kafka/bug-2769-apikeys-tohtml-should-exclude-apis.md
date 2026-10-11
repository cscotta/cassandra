**Commit**: 2bc5e01121 · **Ticket**: KAFKA-17051
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    private static String toHtml() {
        for (ApiKeys key : clientApis()) {
            b.append("<tr>\n");
            b.append("<td>");
            b.append("<a href=\"#The_Messages_" + key.name + "\">" + key.name + "</a>");
            b.append("</td>");
            b.append("<td>");
            b.append(key.id);
            b.append("</td>");
            b.append("</tr>\n");
        }
<p>The following are the numeric codes that the ApiKey in the request can take for each of the below request types.</p>

// After
    static String toHtml() {
        clientApis().stream()
            .filter(apiKey -> !apiKey.messageType.latestVersionUnstable())
            .forEach(apiKey -> {
                b.append("<tr>\n");
                b.append("<td>");
                b.append("<a href=\"#The_Messages_" + apiKey.name + "\">" + apiKey.name + "</a>");
                b.append("</td>");
                b.append("<td>");
                b.append(apiKey.id);
                b.append("</td>");
                b.append("</tr>\n");
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17051 apikeys#tohtml should exclude the apis having unstable latest version (#16480).

**Test**
Test modified in ApiKeysTest.java covering the corrected behaviour.
