package io.lfdt.paladin.poc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

class HexBytesTest {

    @Nested
    @DisplayName("Construction from byte array")
    class FromBytes {

        @Test
        void roundTripsArbitraryBytes() {
            byte[] input = {0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef};
            HexBytes hb = HexBytes.of(input);
            assertEquals("0x0123456789abcdef", hb.toHex());
            assertArrayEquals(input, hb.toByteArray());
        }

        @Test
        void emptyArrayBecomesEmptyHex() {
            HexBytes hb = HexBytes.of(new byte[0]);
            assertEquals("0x", hb.toHex());
            assertTrue(hb.isEmpty());
            assertEquals(0, hb.length());
        }

        @Test
        void preservesLeadingZeros() {
            byte[] input = {0x00, 0x00, 0x01};
            HexBytes hb = HexBytes.of(input);
            assertEquals("0x000001", hb.toHex());
        }

        @Test
        void defensivelyCopiesInputArray() {
            byte[] input = {0x42};
            HexBytes hb = HexBytes.of(input);
            input[0] = 0x00;  // mutate the original
            assertEquals("0x42", hb.toHex());  // HexBytes unaffected
        }

        @Test
        void rejectsNullInput() {
            assertThrows(NullPointerException.class, () -> HexBytes.of(null));
        }
    }

    @Nested
    @DisplayName("Parsing from hex string")
    class FromHex {

        @Test
        void parsesValidHexString() {
            HexBytes hb = HexBytes.fromHex("0xdeadbeef");
            assertArrayEquals(new byte[]{(byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef},
                              hb.toByteArray());
        }

        @Test
        void parsesEmptyHexAsEmptyBytes() {
            HexBytes hb = HexBytes.fromHex("0x");
            assertTrue(hb.isEmpty());
            assertSame(HexBytes.EMPTY, hb);
        }

        @Test
        void rejectsMissingPrefix() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> HexBytes.fromHex("deadbeef"));
            assertTrue(ex.getMessage().contains("0x"));
        }

        @Test
        void rejectsOddLengthBody() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> HexBytes.fromHex("0xabc"));
            assertTrue(ex.getMessage().contains("odd"));
        }

        @Test
        void rejectsNonHexCharacters() {
            assertThrows(IllegalArgumentException.class,
                () -> HexBytes.fromHex("0xzzzz"));
        }

        @Test
        void rejectsNullInput() {
            assertThrows(NullPointerException.class, () -> HexBytes.fromHex(null));
        }

        @Test
        void acceptsLowercaseAndUppercase() {
            HexBytes lower = HexBytes.fromHex("0xabcdef");
            HexBytes upper = HexBytes.fromHex("0xABCDEF");
            assertEquals(lower, upper);
        }

        @Test
        void canonicalOutputIsLowercase() {
            HexBytes hb = HexBytes.fromHex("0xABCDEF");
            assertEquals("0xabcdef", hb.toHex());
        }
    }

    @Nested
    @DisplayName("Equality and hashing")
    class EqualityContract {

        @Test
        void equalBytesAreEqual() {
            HexBytes a = HexBytes.fromHex("0x1234");
            HexBytes b = HexBytes.fromHex("0x1234");
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        void differentBytesAreNotEqual() {
            HexBytes a = HexBytes.fromHex("0x1234");
            HexBytes b = HexBytes.fromHex("0x5678");
            assertNotEquals(a, b);
        }

        @Test
        void emptyEqualsEmpty() {
            assertEquals(HexBytes.EMPTY, HexBytes.fromHex("0x"));
            assertEquals(HexBytes.EMPTY, HexBytes.of(new byte[0]));
        }
    }
}
