## Commit

- Hash: c6f2276e32
- Tickets: KAFKA-17671
- Subject: KAFKA-17671: Add "Using Transactions" section to Kafka design documentation

## Subsystem

docs / design / transactions

## Category

Logic error in condition

## Root cause

The Kafka documentation lacked a dedicated prose section explaining how to use the Kafka transactions API from a developer perspective. The producer transaction methods (`initTransactions`, `beginTransaction`, `commitTransaction`, `abortTransaction`) existed in the API but the design document gave no guidance on their intended usage patterns, error handling, or idempotence semantics. This gap led to common misuse patterns (e.g. not calling `abortTransaction` on exception).

## Before

No section 4.7 "Using Transactions" in `docs/design.html`; sections 4.7–4.10 numbered one lower.

## After

New section 4.7 "Using Transactions" added with code examples and best-practice guidance; subsequent sections renumbered in `docs/toc.html`.

## How to spot

Design documentation that describes the internal implementation of a feature (e.g. the transaction log protocol) but provides no user-facing guidance on how to call the corresponding API correctly.

## Test

Documentation change only; verified by rendering the HTML.
