**Commit**: 7efb58f321 · **Ticket**: KAFKA-16584
**Subsystem**: docs/streams
**Category**: Logic error in condition

**Root cause**
KAFKA-16584 Make log processing summary configurable or debug

Reviewers: Matthias Sax <mjsax@apache.org>, Bill Bejeck <bbejeck@apache.org>

**Before/After**
```java
// Before
                    WINDOW_SIZE_MS_DOC);
    private static final long LOG_SUMMARY_INTERVAL_MS = 2 * 60 * 1000L; // log a summary of processing every 2 minutes

// After
              <li><a class="reference internal" href="#log-summary-interval-ms" id="id40">log.summary.interval.ms</a></li>
          <tr class="row-even"><td>log.summary.interval.ms</td>
            <td>Low</td>
            <td colspan="2">Added to a windows maintainMs to ensure data is not deleted from the log prematurely. Allows for clock drift.</td>
            <td>120000milliseconds  (2 minutes)</td>
          </tr>
    <div class="section" id="log-summary-interval-ms">
      <h4><a class="toc-backref" href="#id40">log.summary.interval.ms</a><a class="headerlink" href="#log-summary-interval-ms" title="Permalink to this headline"></a></h4>
      <blockquote>
        <div>
          This configuration controls the output interval for summary information.
          If greater or equal to 0, the summary log will be output according to the set time interval;
```

**How to spot**
When reviewing docs/streams code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16584 make log processing summary configurable or debug (#16509).

**Test**
No test added.
