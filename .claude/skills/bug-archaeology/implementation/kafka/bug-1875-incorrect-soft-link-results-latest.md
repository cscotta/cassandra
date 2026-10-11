**Commit**: 104fa57933 · **Ticket**: KAFKA-18118
**Subsystem**: tests/docker
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    exec docker exec --user=ducker ducker01 bash -c "${cmd}"

// After
correct_latest_link() {
    local result_dir="${kafka_dir}/results"
    local latest_link="${result_dir}/latest"

    # Correct the link if it's a symbolic link and broken.
    if [[ -L "${latest_link}" ]] && [[ ! -e "${latest_link}" ]]; then
        local latest_test_dirname=$(basename "$(readlink "${latest_link}")")
        unlink "${latest_link}"
        ln -s "${result_dir}/${latest_test_dirname}" "${latest_link}"
    fi
}

```

**How to spot**
When reviewing tests/docker code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18118 fix the incorrect soft link of results/latest (#17992).

**Test**
Test modified in ducker-ak covering the corrected behaviour.
