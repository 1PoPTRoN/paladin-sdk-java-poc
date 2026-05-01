package io.lfdt.paladin.poc;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Thrown when the Paladin server returns a JSON-RPC error envelope.
 * <p>
 * Carries the numeric error code (e.g., PD012229), the human-readable
 * message, and any structured {@code data} field the server provided.
 * The request ID is preserved for correlation with server-side logs.
 */
public final class PaladinRpcException extends PaladinException {

    private final int code;
    private final JsonNode data;
    private final long requestId;

    public PaladinRpcException(int code, String message, JsonNode data, long requestId) {
        super("JSON-RPC error " + code + ": " + message + " (request id=" + requestId + ")");
        this.code = code;
        this.data = data;
        this.requestId = requestId;
    }

    public int code() {
        return code;
    }

    public JsonNode data() {
        return data;
    }

    public long requestId() {
        return requestId;
    }
}
