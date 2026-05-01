package io.lfdt.paladin.poc.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The type of a Paladin transaction.
 * <p>
 * Modeled as an enum rather than a sealed interface for this POC since
 * the value space is closed and Jackson handles enum serialization with
 * zero configuration. The full SDK may revisit this if domain-specific
 * transaction subtypes emerge.
 */
public enum TransactionType {
    PRIVATE("private"),
    PUBLIC("public");

    private final String wireValue;

    TransactionType(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String wireValue() {
        return wireValue;
    }
}
