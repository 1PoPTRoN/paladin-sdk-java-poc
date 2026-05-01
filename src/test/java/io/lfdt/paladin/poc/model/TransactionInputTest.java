package io.lfdt.paladin.poc.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.lfdt.paladin.poc.HexBytes;
import io.lfdt.paladin.poc.HexBytesDeserializer;
import io.lfdt.paladin.poc.HexBytesSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.junit.jupiter.api.Assertions.*;

class TransactionInputTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        SimpleModule hexModule = new SimpleModule()
            .addSerializer(HexBytes.class, new HexBytesSerializer())
            .addDeserializer(HexBytes.class, new HexBytesDeserializer());
        mapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(hexModule)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Test
    void serializesAllFieldsWhenPresent() throws Exception {
        TransactionInput tx = new TransactionInput(
            Optional.of("idem-key-001"),
            TransactionType.PRIVATE,
            Optional.of("noto"),
            "alice@node1",
            Optional.of(HexBytes.fromHex("0xabcdef")),
            "transfer",
            Optional.of(HexBytes.fromHex("0x010203"))
        );

        String json = mapper.writeValueAsString(tx);

        assertThatJson(json).isEqualTo("""
            {
              "idempotencyKey": "idem-key-001",
              "type": "private",
              "domain": "noto",
              "from": "alice@node1",
              "to": "0xabcdef",
              "function": "transfer",
              "data": "0x010203"
            }
            """);
    }

    @Test
    void omitsAbsentOptionalFields() throws Exception {
        TransactionInput tx = TransactionInput.privateCall("alice@node1", "balanceOf");
        String json = mapper.writeValueAsString(tx);

        assertThatJson(json).isEqualTo("""
            {
              "type": "private",
              "from": "alice@node1",
              "function": "balanceOf"
            }
            """);
    }

    @Test
    void roundTripsAllFields() throws Exception {
        TransactionInput original = new TransactionInput(
            Optional.of("idem-001"),
            TransactionType.PRIVATE,
            Optional.of("noto"),
            "alice@node1",
            Optional.of(HexBytes.fromHex("0xdeadbeef")),
            "transfer",
            Optional.of(HexBytes.fromHex("0xcafebabe"))
        );

        String json = mapper.writeValueAsString(original);
        TransactionInput decoded = mapper.readValue(json, TransactionInput.class);

        assertEquals(original, decoded);
    }

    @Test
    void roundTripsMinimalFields() throws Exception {
        TransactionInput original = TransactionInput.privateCall("bob@node2", "totalSupply");

        String json = mapper.writeValueAsString(original);
        TransactionInput decoded = mapper.readValue(json, TransactionInput.class);

        assertEquals(original, decoded);
    }

    @Test
    void deserializesPublicTransactionType() throws Exception {
        String json = """
            {
              "type": "public",
              "from": "treasury@node1",
              "function": "publicMint"
            }
            """;
        TransactionInput tx = mapper.readValue(json, TransactionInput.class);

        assertEquals(TransactionType.PUBLIC, tx.type());
        assertEquals("treasury@node1", tx.from());
        assertTrue(tx.idempotencyKey().isEmpty());
    }

    @Test
    void hexBytesFieldsRoundTripCorrectly() throws Exception {
        HexBytes payload = HexBytes.fromHex("0x000102030405060708090a0b0c0d0e0f");
        TransactionInput tx = new TransactionInput(
            Optional.empty(),
            TransactionType.PRIVATE,
            Optional.empty(),
            "alice@node1",
            Optional.empty(),
            "method",
            Optional.of(payload)
        );

        String json = mapper.writeValueAsString(tx);
        TransactionInput decoded = mapper.readValue(json, TransactionInput.class);

        assertEquals(payload, decoded.data().orElseThrow());
    }
}
