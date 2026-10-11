## Commit

- Hash: d4aee71e36
- Tickets: KAFKA-18465
- Subject: KAFKA-18465: Remove saslHandshakeRequestEnable, minRecordVersion, and canUseTopicIds parameters

## Subsystem

clients / network / SASL / request builders

## Category

Wrong constant / default value

## Root cause

Several classes carried parameters that were meaningful only during version negotiation before a cluster was fully upgraded:
- `saslHandshakeRequestEnable` in `SaslChannelBuilder`/`SaslClientAuthenticator` — always `true` for any current broker
- `RecordVersion minRecordVersion` filter in `ChannelBuilders` — forced message-format downgrade; no longer needed
- `canUseTopicIds` in `AlterPartitionRequest`/`WriteTxnMarkersRequest`/`OffsetsForLeaderEpochRequest` — always `true` on current brokers

Keeping the parameters as constants that were always set to the same value introduced dead branches and made the API surface misleading.

## Before

Constructors and builder methods accepted the above parameters; callers always passed the always-`true` value.

## After

Parameters removed; the always-`true` behaviour is hard-coded internally.

## How to spot

Constructor or builder parameters that every call site passes as a literal `true`/`false` constant with no variation. These should be removed and their value inlined.

## Test

Channel builder and SASL authenticator unit tests updated; no behavioural change.
