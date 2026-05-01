package io.lfdt.paladin.poc;

import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Immutable wrapper around a byte array with stable hex-string representation.
 * <p>
 * Paladin's JSON-RPC API uses 0x-prefixed hex strings throughout: transaction
 * IDs, contract addresses, ABI payloads, signatures, state hashes. This type
 * is the SDK's canonical representation for any byte-valued field crossing
 * the wire.
 * <p>
 * Construction policies:
 * <ul>
 *   <li>The 0x prefix is required when parsing from a string.</li>
 *   <li>The empty byte array is valid and serializes as {@code "0x"}.</li>
 *   <li>Odd-length input is rejected to prevent silent corruption.</li>
 *   <li>Leading zeros are preserved verbatim (no stripping).</li>
 * </ul>
 * <p>
 * Defensive copies are made on construction and on {@link #toByteArray()}
 * to preserve immutability.
 */
public final class HexBytes {

    private static final HexFormat HEX = HexFormat.of();
    private static final String PREFIX = "0x";

    /** The canonical empty value, equivalent to {@code "0x"}. */
    public static final HexBytes EMPTY = new HexBytes(new byte[0]);

    private final byte[] bytes;

    private HexBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    /**
     * Construct from a raw byte array. The input is defensively copied.
     */
    public static HexBytes of(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length == 0) {
            return EMPTY;
        }
        return new HexBytes(Arrays.copyOf(bytes, bytes.length));
    }

    /**
     * Parse from a 0x-prefixed hex string.
     *
     * @throws IllegalArgumentException if the string does not start with "0x"
     *         or has an odd number of hex characters after the prefix
     */
    public static HexBytes fromHex(String hex) {
        Objects.requireNonNull(hex, "hex");
        if (!hex.startsWith(PREFIX)) {
            throw new IllegalArgumentException(
                "Hex string must start with '0x' but was: " + hex);
        }
        String body = hex.substring(PREFIX.length());
        if (body.isEmpty()) {
            return EMPTY;
        }
        if ((body.length() & 1) != 0) {
            throw new IllegalArgumentException(
                "Hex string has odd number of digits (expected pairs of hex chars): " + hex);
        }
        try {
            return new HexBytes(HEX.parseHex(body));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid hex characters in: " + hex, e);
        }
    }

    /**
     * @return a defensive copy of the underlying byte array
     */
    public byte[] toByteArray() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    /**
     * @return the canonical 0x-prefixed hex string, lowercase
     */
    public String toHex() {
        return PREFIX + HEX.formatHex(bytes);
    }

    public int length() {
        return bytes.length;
    }

    public boolean isEmpty() {
        return bytes.length == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HexBytes other)) return false;
        return Arrays.equals(bytes, other.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public String toString() {
        return toHex();
    }
}
