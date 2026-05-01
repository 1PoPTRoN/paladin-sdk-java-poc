package io.lfdt.paladin.poc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.lfdt.paladin.poc.model.TransactionInput;
import io.lfdt.paladin.poc.model.TransactionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Differential contract test: asserts the Java SDK produces a wire-level
 * JSON-RPC request body equivalent to what the official TypeScript SDK
 * produces for the same logical operation.
 * <p>
 * The reference fixture in {@code src/test/resources/fixtures/} was
 * captured by the {@code ts-fixture-generator} subproject, which runs
 * the published {@code @lfdecentralizedtrust/paladin-sdk} package
 * against a mock server and persists the exact request body the SDK
 * sent. This test loads that fixture and confirms the Java
 * {@link JsonRpcTransport} sends an equivalent body.
 * <p>
 * "Equivalent" means matching {@code jsonrpc}, {@code method}, and
 * {@code params}. The {@code id} field is dynamically allocated per-call
 * by both transports and is asserted as "any positive integer" rather
 * than as a literal value.
 */
class ContractTest {

    private static final String FIXTURE_RESOURCE = "/fixtures/ptx_sendTransaction.json";

    private WireMockServer wireMock;
    private JsonRpcTransport transport;
    private ObjectMapper mapper;

    @BeforeEach
    void startServer() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
        transport = new JsonRpcTransport("http://localhost:" + wireMock.port());
        mapper = new ObjectMapper();
    }

    @AfterEach
    void stopServer() {
        wireMock.stop();
    }

    @Test
    @DisplayName("Java SDK ptx_sendTransaction produces wire-equivalent body to TypeScript SDK")
    void javaSdkMatchesTypeScriptSdkWireFormat() throws Exception {
        // Stub the mock server to return a syntactically-valid JSON-RPC success
        // response. The Java SDK doesn't care what the result is for this test;
        // we only care about what it sent.
        wireMock.stubFor(post(urlEqualTo("/")).willReturn(okJson("""
            {"jsonrpc":"2.0","id":1,"result":"0x0000000000000000000000000000000000000000000000000000000000000001"}
            """)));

        // Build the equivalent TransactionInput. These field values must match
        // the TRANSACTION_INPUT object in ts-fixture-generator/generate.js
        // exactly. If the generator changes, this test must change too.
        TransactionInput tx = new TransactionInput(
            Optional.of("idem-key-001"),
            TransactionType.PRIVATE,
            Optional.of("noto"),
            "alice@node1",
            Optional.of(HexBytes.fromHex("0xabcdef")),
            "transfer",
            Optional.of(HexBytes.fromHex("0x010203"))
        );

        // Invoke the Java transport with the same method name and params shape.
        transport.call(
            "ptx_sendTransaction",
            List.of(tx),
            new TypeReference<String>() {}
        ).join();

        // Capture the body the Java SDK actually sent.
        String javaSdkBody = wireMock.getAllServeEvents().getFirst()
            .getRequest()
            .getBodyAsString();

        // Load the reference fixture captured from the TypeScript SDK.
        String typescriptSdkBody = loadFixture();

        // Assert wire-level equivalence. JSON-Unit handles whitespace
        // differences and field order. We use IGNORING_ARRAY_ORDER as a
        // conservative default even though the params array order matters
        // here (it's a single-element array, so it can't reorder).
        assertThatJson(javaSdkBody)
            .node("jsonrpc").isString().isEqualTo("2.0");

        assertThatJson(javaSdkBody)
            .node("method").isString().isEqualTo("ptx_sendTransaction");

        // The id field is dynamic. Assert it exists and is a positive integer
        // rather than asserting a literal value.
        JsonNode javaBodyNode = mapper.readTree(javaSdkBody);
        assertThat(javaBodyNode.get("id").isNumber()).isTrue();
        assertThat(javaBodyNode.get("id").asLong()).isPositive();

        // The params node is the load-bearing assertion. The TS fixture's
        // params[0] is what defines wire-format equivalence.
        JsonNode tsBodyNode = mapper.readTree(typescriptSdkBody);
        assertThatJson(javaBodyNode.get("params"))
            .isEqualTo(tsBodyNode.get("params"));
    }

    @Test
    @DisplayName("Captured fixture has the expected stable structure")
    void fixtureStructureIsStable() throws IOException {
        // Sanity check: the fixture itself has the shape we expect.
        // Catches a corrupted or accidentally-modified fixture file before
        // it confuses the main test.
        String fixtureBody = loadFixture();
        JsonNode fixture = mapper.readTree(fixtureBody);

        assertThat(fixture.has("jsonrpc")).isTrue();
        assertThat(fixture.get("jsonrpc").asText()).isEqualTo("2.0");
        assertThat(fixture.has("method")).isTrue();
        assertThat(fixture.get("method").asText()).isEqualTo("ptx_sendTransaction");
        assertThat(fixture.has("params")).isTrue();
        assertThat(fixture.get("params").isArray()).isTrue();
        assertThat(fixture.get("params").size()).isEqualTo(1);

        // The fixture must NOT have an id field. If it does, the generator
        // regression-bug from Gate 5 has crept back in.
        assertThat(fixture.has("id")).isFalse();
    }

    private String loadFixture() throws IOException {
        try (InputStream stream = getClass().getResourceAsStream(FIXTURE_RESOURCE)) {
            if (stream == null) {
                throw new IOException("Fixture resource not found: " + FIXTURE_RESOURCE);
            }
            return new String(stream.readAllBytes());
        }
    }
}
