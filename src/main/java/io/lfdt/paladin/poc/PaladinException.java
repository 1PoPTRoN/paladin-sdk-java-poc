package io.lfdt.paladin.poc;

/**
 * Base exception type for the Paladin SDK.
 * <p>
 * All SDK exceptions extend this class so applications can catch
 * Paladin-specific failures without catching unrelated runtime exceptions.
 */
public abstract class PaladinException extends RuntimeException {

    protected PaladinException(String message) {
        super(message);
    }

    protected PaladinException(String message, Throwable cause) {
        super(message, cause);
    }
}
