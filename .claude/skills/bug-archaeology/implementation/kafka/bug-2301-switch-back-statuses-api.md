**Commit**: 5377595a5f · **Ticket**: KAFKA-17673
**Subsystem**: .github/actions
**Category**: Logic error in condition

**Root cause**
This reverts parts of #17299 related to the checks API 

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
name: "Update Check Run"
description: "Update the status of a commit check using the GH CLI. See https://docs.github.com/en/rest/checks/runs?apiVersion=2022-11-28#create-a-check-run"
  title:
    description: "The title of the status check"
  status:
    description: "The status of the check. Can be one of: queued, in_progress, completed, waiting, requested, pending"
  conclusion:
    description: "Required if status is 'completed'. Can be one of: action_required, cancelled, failure, neutral, success, skipped, stale, timed_out"
    required: false
    - if: inputs.conclusion == ''
      shell: bash
      env:

// After
name: "Update Commit Status Check"
description: "Update the status of a commit check using the GH CLI"
  context:
    description: "The name of the status check"
  state:
    description: "The state of the check. Can be one of: error, failure, pending, success"
    - name: Update Check
        /repos/${{ inputs.repository }}/statuses/${{ inputs.commit_sha }} \
        -f "state=${{ inputs.state }}" -f "target_url=${{ inputs.url }}" \
        -f "description=${{ inputs.description }}" \
        -f "context=${{ inputs.context }}"
        uses: ./.github/actions/gh-api-update-status
```

**How to spot**
When reviewing .github/actions code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17673 switch back to statuses api (#17336).

**Test**
No test added.
