package io.lfdt.paladin.poc;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Serializes {@link HexBytes} as a 0x-prefixed lowercase hex string.
 */
public final class HexBytesSerializer extends JsonSerializer<HexBytes> {

    @Override
    public void serialize(HexBytes value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        gen.writeString(value.toHex());
    }
}
