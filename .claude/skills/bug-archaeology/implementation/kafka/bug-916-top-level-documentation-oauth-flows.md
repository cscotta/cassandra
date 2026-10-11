**Commit**: 3c902bacc0 · **Ticket**: KAFKA-19152
**Subsystem**: docs/configuration.html
**Category**: Logic error in condition

**Root cause**
Adds documentation to support the OAuth additions from KIP-768 and
KIP-1139. The existing documentation is heavily geared toward Kafka's support for
non-production OAuth usage. Since this mode is still supported, it
should not be removed. However, with the addition of the production
OAuth usage, the documentation is less than succinct because it has a
bit of a split personality issue.

**Before/After**
```java
// Before
                        <a href="#security_sasl_scram_brokerconfig">SCRAM</a> or
                        <a href="#security_sasl_oauthbearer_brokerconfig">OAUTHBEARER</a> for example broker configurations.</p></li>
                                <a href="#security_sasl_scram_clientconfig">SCRAM</a> or
                                <a href="#security_sasl_oauthbearer_clientconfig">OAUTHBEARER</a> for example configurations.</p></li>
                                    <a href="#security_sasl_scram_clientconfig">SCRAM</a> or
                                    <a href="#security_sasl_oauthbearer_clientconfig">OAUTHBEARER</a>.

// After
    <li>
      <h4><a id="org.apache.kafka.sasl.oauthbearer.allowed.files"></a><a id="systemproperties_org.apache.kafka.sasl.oauthbearer.allowed.files" href="#systemproperties_org.apache.kafka.sasl.oauthbearer.allowed.files">org.apache.kafka.sasl.oauthbearer.allowed.files</a></h4>
      <p>This system property is used to determine which files, if any, are allowed to be read by the SASL OAUTHBEARER plugin. This property accepts comma-separated list of files. By default the value is an empty list.
      <p>If users want to enable some files, users need to explicitly set the system property like below.
      <p><pre><code class="language-bash">-Dorg.apache.kafka.sasl.oauthbearer.allowed.files=/tmp/token,/tmp/private_key.pem</code></pre>
      <table><tbody>
      <tr><th>Since:</th><td>4.1.0</td></tr>
      <tr><th>Default Value:</th><td></td></tr>
      </tbody></table>
    </li>
                        <a href="#security_sasl_scram_brokerconfig">SCRAM</a>, or
                        <a href="#security_sasl_oauthbearer_non_production_brokerconfig">non-production</a>/<a href="#security_sasl_oauthbearer_production_brokerconfig">production</a> OAUTHBEARER for example broker configurations.</p></li>
```

**How to spot**
When reviewing docs/configuration.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19152: add top-level documentation for oauth flows (#20025).

**Test**
No test added.
