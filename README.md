# Paladin Java SDK — Proof of Concept

Pre-mentorship validation of the architectural foundation for [LFX Mentorship 2026 — Build and release a Java SDK for Paladin (Issue #64)](https://github.com/LF-Decentralized-Trust-Mentorships/mentorship-program/issues/64).

This is **not** the SDK. It is a 6-file proof of concept that validates the three highest-risk design decisions in the proposal *before* the 20-week mentorship clock starts. Building these in advance reduces the risk that the SDK design hits a fundamental architectural problem during the mentorship itself.

## What this POC validates

| Claim from the proposal | How this POC validates it |
|---|---|
| `java.net.http.HttpClient` is sufficient as the SDK's transport (no Apache HttpClient, no OkHttp) | A working `JsonRpcTransport` (~150 LOC) handling JSON-RPC envelopes, async dispatch, and three exception types |
| Java records with `Optional<T>` and custom Jackson codecs round-trip cleanly to Paladin's wire format | A `TransactionInput` record with `Optional` and `HexBytes` fields, plus 23 round-trip and edge-case tests |
| The differential-test oracle approach (§5.2 of the proposal) is mechanically sound | A WireMock-based contract test that asserts wire-level equivalence between the Java SDK and the official `@lfdecentralizedtrust/paladin-sdk` TypeScript SDK |

37 tests pass. Coverage spans unit-level edge cases, Jackson round-trip integration, and the differential contract test.

## What this POC does NOT cover

The POC's scope boundary is as important as its scope. Out of scope:

- WebSocket transport, reconnect logic, ack/nack delivery (proposal §4.2)
- Domain-specific builders for Noto, Pente, Zeto (proposal §4.3)
- The remaining 8 namespaces and 98 RPC methods (proposal §4.1)
- Maven Central publishing, GitHub Actions release workflow (proposal §4.4)
- Integration tests against a real three-node Paladin network

These are mentorship deliverables. Pre-building them would convert this POC into a request for compensation for work already done, which is not the purpose of an LFX mentorship.

## Running the tests

Requires JDK 17+ (tested on JDK 26 / Temurin). No other prerequisites — Gradle and all dependencies are pulled by the wrapper.

```bash
./gradlew test
```

Expected: 37 tests pass in roughly 1 second. The contract test loads a fixture captured from the TypeScript SDK and asserts the Java SDK produces a wire-equivalent JSON-RPC request body.

## Regenerating the contract-test fixture

The reference fixture in `src/test/resources/fixtures/ptx_sendTransaction.json` was captured from `@lfdecentralizedtrust/paladin-sdk@0.15.0` running against a mock HTTP server. To regenerate it (e.g., after a TS SDK release):

```bash
cd ts-fixture-generator
npm install
npm run generate
cp fixtures/ptx_sendTransaction.json ../src/test/resources/fixtures/
```

The fixture is deterministic across runs — the dynamically-allocated request `id` is stripped during capture so the fixture acts as a stable contract.

## Repository layout
paladin-sdk-java-poc/
├── src/main/java/io/lfdt/paladin/poc/
│   ├── JsonRpcTransport.java          # JSON-RPC transport over java.net.http
│   ├── HexBytes.java                  # 0x-prefixed hex byte value type
│   ├── HexBytesSerializer.java        # Jackson serializer
│   ├── HexBytesDeserializer.java      # Jackson deserializer
│   ├── PaladinException.java          # base exception
│   ├── PaladinRpcException.java       # JSON-RPC error envelope
│   ├── PaladinTransportException.java # network-level failures
│   └── model/
│       ├── TransactionInput.java      # ptx_sendTransaction input record
│       └── TransactionType.java       # PRIVATE | PUBLIC enum
├── src/test/java/io/lfdt/paladin/poc/
│   ├── ContractTest.java              # differential test vs TypeScript SDK
│   ├── HexBytesTest.java              # value-type edge cases
│   ├── HexBytesJacksonTest.java       # Jackson round-trip
│   ├── JsonRpcTransportTest.java      # transport failure modes
│   ├── SmokeTest.java                 # build verification
│   └── model/
│       └── TransactionInputTest.java  # record + Optional round-trip
├── src/test/resources/fixtures/
│   └── ptx_sendTransaction.json       # reference wire format from TS SDK
└── ts-fixture-generator/
├── generate.js                    # captures fixture from TS SDK
└── package.json                   # pinned to @lfdecentralizedtrust/paladin-sdk@0.15.0

## Design decisions (and what's deferred to the mentorship)

**Decisions made and validated in this POC:**

- `CompletableFuture<R>` as the async return type, via `HttpClient.sendAsync()`
- Records (JEP 395) for value types; `Optional<T>` for nullable fields
- `@JsonInclude(NON_ABSENT)` so empty Optionals are omitted from wire output
- Custom Jackson `JsonSerializer`/`JsonDeserializer` for hex-byte fields, rejecting odd-length and missing-prefix input
- Three-class exception hierarchy mapping to three failure modes (JSON-RPC error, transport failure, base type)

**Decisions deliberately deferred to the mentorship:**

- Sealed interfaces for sum types in receipts and prepared transactions (proposal §4.1.4) — POC didn't need them yet
- Virtual-thread opt-in for blocking facade users on JDK 21+ (proposal §3.3.3)
- Module split: `paladin-sdk-core`, `paladin-sdk-ws`, `paladin-sdk-domain-*`, `paladin-sdk-bom` (proposal §4.1.1)
- Maven Central publishing under `io.lfdt.paladin` (proposal §4.4.2)

## About this work

Built by [Arpit Raj](https://github.com/1PoPTRoN) as pre-work for an LFX Mentorship 2026 application to the LFDT Paladin project, mentored by Anna McAllister and Matthew Whitehead at Kaleido.

The proposal (forthcoming, link to be added) documents the full 20-week plan; this POC validates that the load-bearing architectural choices in §3.3, §4.1, and §5.2 of that proposal work in practice before committing to the implementation timeline.

## License

Apache License 2.0 — matches the upstream Paladin project. See [LICENSE](LICENSE) for the full text.
