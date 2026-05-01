package io.lfdt.paladin.poc.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.lfdt.paladin.poc.HexBytes;

import java.util.Optional;

/**
 * The input payload for {@code ptx_sendTransaction}.
 * <p>
 * Mirrors the {@code ITransactionInput} type in the TypeScript SDK
 * ({@code sdk/typescript/src/interfaces/transaction.ts}) at the wire level.
 * Optional fields use {@link Optional} so the type system makes nullability
 * explicit, and {@link JsonInclude.Include#NON_ABSENT} ensures empty
 * Optionals are omitted from serialized output rather than emitting
 * {@code null} or {@code "Optional.empty"}.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public record TransactionInput(
    @JsonProperty("idempotencyKey") Optional<String> idempotencyKey,
    @JsonProperty("type") TransactionType type,
    @JsonProperty("domain") Optional<String> domain,
    @JsonProperty("from") String from,
    @JsonProperty("to") Optional<HexBytes> to,
    @JsonProperty("function") String function,
    @JsonProperty("data") Optional<HexBytes> data
) {
    /**
     * Convenience constructor for the common "no idempotency, no specific to-address"
     * case used in many test scenarios.
     */
    public static TransactionInput privateCall(String from, String function) {
        return new TransactionInput(
            Optional.empty(),
            TransactionType.PRIVATE,
            Optional.empty(),
            from,
            Optional.empty(),
            function,
            Optional.empty()
        );
    }
}
