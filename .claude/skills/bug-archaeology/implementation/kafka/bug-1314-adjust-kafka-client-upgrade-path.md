**Commit**: f0f7382ff7 · **Ticket**: KAFKA-18422
**Subsystem**: docs/upgrade.html
**Category**: Resource leak

**Root cause**
This patch adds a section about upgrading clients to the upgrade notes. Reviewers: Ismael Juma <ismael@juma.me.uk>, David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
<h4><a id="upgrade_4_1_0" href="#upgrade_4_1_0">Upgrading to 4.1.0 from any version 3.3.x through 4.0.x</a></h4>
    <h5><a id="upgrade_410_notable" href="#upgrade_410_notable">Notable changes in 4.1.0</a></h5>
<h4><a id="upgrade_4_0_0" href="#upgrade_4_0_0">Upgrading to 4.0.0 from any version 3.3.x through 3.9.x</a></h4>
    <li>For the Kafka client upgrade path, note that many deprecated APIs were removed in Kafka 4.0. Additionally, upgrading directly to 4.x from certain versions is not feasible.
        For more information, please refer to <a href="https://cwiki.apache.org/confluence/x/y4kgF">KIP-1124</a>.
    </li>
    <h5><a id="upgrade_400_notable" href="#upgrade_400_notable">Notable changes in 4.0.0</a></h5>

// After
<h4><a id="upgrade_4_1_0" href="#upgrade_4_1_0">Upgrading to 4.1.0</a></h4>

<h5><a id="upgrade_4_1_0" href="#upgrade_4_1_0">Upgrading Servers to 4.1.0 from any version 3.3.x through 4.0.x</a></h5>
    <h6><a id="upgrade_410_notable" href="#upgrade_410_notable">Notable changes in 4.1.0</a></h6>

<h4><a id="upgrade_4_0_0" href="#upgrade_4_0_0">Upgrading to 4.0.0</a></h4>

<h5><a id="upgrade_clients_4_0_0" href="#upgrade_clients_4_0_0">Upgrading Clients to 4.0.0</a></h5>

<p><b>For a rolling upgrade:</b></p>

<ol>
```

**How to spot**
When reviewing docs/upgrade.html code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18422 adjust kafka client upgrade path section (#19119).

**Test**
No test added.
