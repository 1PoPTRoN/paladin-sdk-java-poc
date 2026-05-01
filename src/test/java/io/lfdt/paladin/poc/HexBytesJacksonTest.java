package io.lfdt.paladin.poc;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HexBytesJacksonTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        SimpleModule module = new SimpleModule()
            .addSerializer(HexBytes.class, new HexBytesSerializer())
            .addDeserializer(HexBytes.class, new HexBytesDeserializer());
        mapper = new ObjectMapper().registerModule(module);
    }

    @Test
    void serializesAsQuotedHexString() throws Exception {
        HexBytes hb = HexBytes.fromHex("0xdeadbeef");
        String json = mapper.writeValueAsString(hb);
        assertEquals("\"0xdeadbeef\"", json);
    }

    @Test
    void serializesEmptyAsZeroX() throws Exception {
        String json = mapper.writeValueAsString(HexBytes.EMPTY);
        assertEquals("\"0x\"", json);
    }

    @Test
    void deserializesValidHexString() throws Exception {
        HexBytes hb = mapper.readValue("\"0xcafebabe\"", HexBytes.class);
        assertEquals(HexBytes.fromHex("0xcafebabe"), hb);
    }

    @Test
    void deserializesEmptyHexString() throws Exception {
        HexBytes hb = mapper.readValue("\"0x\"", HexBytes.class);
        assertSame(HexBytes.EMPTY, hb);
    }

    @Test
    void rejectsMalformedHexAsJsonMappingException() {
        assertThrows(JsonMappingException.class,
            () -> mapper.readValue("\"not-hex\"", HexBytes.class));
    }

    @Test
    void rejectsNonStringTokenAsJsonMappingException() {
        assertThrows(JsonMappingException.class,
            () -> mapper.readValue("12345", HexBytes.class));
    }

    @Test
    void roundTripPreservesValue() throws Exception {
        HexBytes original = HexBytes.fromHex("0x000102030405060708090a0b0c0d0e0f");
        String json = mapper.writeValueAsString(original);
        HexBytes decoded = mapper.readValue(json, HexBytes.class);
        assertEquals(original, decoded);
    }
}
