**Commit**: eafb92bdce · **Ticket**: KAFKA-17193
**Subsystem**: .github/actions
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
      uses: gradle/actions/setup-gradle@v4
      uses: aquasecurity/trivy-action@master
      uses: aquasecurity/trivy-action@master
      uses: docker/setup-qemu-action@v3
      uses: docker/setup-buildx-action@v3
      uses: docker/login-action@v3
      uses: docker/setup-qemu-action@v3
      uses: docker/setup-buildx-action@v3
      uses: docker/login-action@v3
        uses: aquasecurity/trivy-action@master

// After
      uses: gradle/actions/setup-gradle@af1da67850ed9a4cedd57bfd976089dd991e2582 # v4.0.0
      uses: aquasecurity/trivy-action@6e7b7d1fd3e4fef0c5fa8cce1229c54b2c9bd0d8 # v0.24.0
      uses: aquasecurity/trivy-action@6e7b7d1fd3e4fef0c5fa8cce1229c54b2c9bd0d8 # v0.24.0
      uses: docker/setup-qemu-action@49b3bc8e6bdd4a60e6116a5414239cba5943d3cf # v3.2.0
      uses: docker/setup-buildx-action@988b5a0280414f521da01fcc63a27aeeb4b104db # v3.6.1
      uses: docker/login-action@9780b0c442fbb1117ed29e0efdff1e18412f7567 # v3.3.0
      uses: docker/setup-qemu-action@49b3bc8e6bdd4a60e6116a5414239cba5943d3cf # v3.2.0
      uses: docker/setup-buildx-action@988b5a0280414f521da01fcc63a27aeeb4b104db # v3.6.1
      uses: docker/login-action@9780b0c442fbb1117ed29e0efdff1e18412f7567 # v3.3.0
        uses: aquasecurity/trivy-action@6e7b7d1fd3e4fef0c5fa8cce1229c54b2c9bd0d8 # v0.24.0
```

**How to spot**
When reviewing .github/actions code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17193: pin all external github actions to the specific git hash (#16960).

**Test**
Test modified in docker_build_and_test.yml covering the corrected behaviour.
