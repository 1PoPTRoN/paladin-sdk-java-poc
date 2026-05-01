package io.lfdt.paladin.poc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The single seam between the Paladin SDK and the network.
 * <p>
 * All namespace interfaces (Ptx, Pgroup, Keymgr, etc.) eventually call
 * into one shared {@code JsonRpcTransport}. The transport handles
 * request-id allocation, JSON-RPC envelope construction, HTTP dispatch,
 * and response parsing. Higher layers translate domain-specific
 * parameters and result types but do not touch the network directly.
 * <p>
 * Thread-safe. The underlying {@link HttpClient} and {@link ObjectMapper}
 * are both designed for shared, long-lived use.
 */
public final class JsonRpcTransport {

    private static final Logger log = LoggerFactory.getLogger(JsonRpcTransport.class);

    private static final String JSON_RPC_VERSION = "2.0";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final URI endpoint;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final AtomicLong requestIdGenerator;
    private final Duration requestTimeout;

    public JsonRpcTransport(String endpoint) {
        this(URI.create(endpoint), defaultHttpClient(), defaultMapper(), DEFAULT_TIMEOUT);
    }

    JsonRpcTransport(URI endpoint, HttpClient httpClient, ObjectMapper mapper, Duration requestTimeout) {
        this.endpoint = endpoint;
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.requestIdGenerator = new AtomicLong(1);
        this.requestTimeout = requestTimeout;
    }

    /**
     * Invoke a JSON-RPC method and deserialize the result to the given type.
     *
     * @param method the JSON-RPC method name (e.g., "ptx_sendTransaction")
     * @param params the positional parameters; null is treated as an empty list
     * @param returnType the type reference for deserializing the result
     * @return a future that completes with the deserialized result, or fails
     *         with {@link PaladinRpcException} or {@link PaladinTransportException}
     */
    public <R> CompletableFuture<R> call(String method, List<?> params, TypeReference<R> returnType) {
        long id = requestIdGenerator.getAndIncrement();
        Map<String, Object> envelope = buildEnvelope(method, params, id);

        String body;
        try {
            body = mapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(
                new PaladinTransportException("Failed to serialize JSON-RPC request: " + method, e));
        }

        log.debug("JSON-RPC request id={} method={}", id, method);

        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(endpoint)
            .timeout(requestTimeout)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
            .handle((response, throwable) -> {
                if (throwable != null) {
                    throw new PaladinTransportException(
                        "JSON-RPC transport failed for method " + method, throwable);
                }
                return parseResponse(response, returnType, method, id);
            });
    }

    private <R> R parseResponse(HttpResponse<String> response,
                                 TypeReference<R> returnType,
                                 String method,
                                 long id) {
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new PaladinTransportException(
                "HTTP " + status + " from Paladin endpoint for method " + method);
        }

        JsonNode root;
        try {
            root = mapper.readTree(response.body());
        } catch (JsonProcessingException e) {
            throw new PaladinTransportException(
                "Malformed JSON response from Paladin for method " + method, e);
        }

        JsonNode error = root.get("error");
        if (error != null && !error.isNull()) {
            int errorCode = error.path("code").asInt();
            String errorMessage = error.path("message").asText("(no message)");
            JsonNode errorData = error.get("data");
            throw new PaladinRpcException(errorCode, errorMessage, errorData, id);
        }

        JsonNode result = root.get("result");
        if (result == null || result.isNull()) {
            // Some methods legitimately return null; let Jackson handle that
            // by deserializing the null node.
            result = mapper.nullNode();
        }

        try {
            return mapper.treeToValue(result, mapper.getTypeFactory().constructType(returnType));
        } catch (JsonProcessingException e) {
            throw new PaladinTransportException(
                "Failed to deserialize JSON-RPC result for method " + method, e);
        }
    }

    private Map<String, Object> buildEnvelope(String method, List<?> params, long id) {
        Map<String, Object> envelope = new HashMap<>(4);
        envelope.put("jsonrpc", JSON_RPC_VERSION);
        envelope.put("id", id);
        envelope.put("method", method);
        envelope.put("params", params == null ? List.of() : params);
        return envelope;
    }

    private static HttpClient defaultHttpClient() {
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    }

    private static ObjectMapper defaultMapper() {
        return new ObjectMapper()
            .registerModule(new Jdk8Module())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
