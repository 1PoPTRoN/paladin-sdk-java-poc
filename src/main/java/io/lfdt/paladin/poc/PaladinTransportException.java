package io.lfdt.paladin.poc;

/**
 * Thrown when the SDK fails to communicate with the Paladin server at the
 * transport layer: connection refused, timeout, malformed response, or
 * non-2xx HTTP status outside the JSON-RPC envelope.
 */
public final class PaladinTransportException extends PaladinException {

    public PaladinTransportException(String message) {
        super(message);
    }

    public PaladinTransportException(String message, Throwable cause) {
        super(message, cause);
    }
}
