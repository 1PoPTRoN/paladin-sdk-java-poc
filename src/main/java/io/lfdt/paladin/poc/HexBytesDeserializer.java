package io.lfdt.paladin.poc;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.io.IOException;

/**
 * Deserializes a 0x-prefixed hex string into {@link HexBytes}.
 * <p>
 * Rejects malformed input rather than silently producing wrong bytes:
 * a missing prefix, odd-length body, or non-hex characters all raise
 * {@link JsonMappingException} with a clear message.
 */
public final class HexBytesDeserializer extends JsonDeserializer<HexBytes> {

    @Override
    public HexBytes deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() != JsonToken.VALUE_STRING) {
            throw JsonMappingException.from(p,
                "Expected JSON string for HexBytes but got " + p.currentToken());
        }
        String text = p.getText();
        try {
            return HexBytes.fromHex(text);
        } catch (IllegalArgumentException e) {
            throw JsonMappingException.from(p, e.getMessage(), e);
        }
    }
}
