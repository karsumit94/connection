package connection;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A class that can be used for Base64 encoding/decoding
 */
public class Base64 {

    private static final byte PLACE_HOLDER = (byte) 200;

    /**
     * Encode a given string into base64
     *
     * @param raw
     * @return
     * @throws Exception
     */
    public static String encode(String raw) throws Exception {
        if ((raw == null) || raw.trim().length() == 0) {
            return raw;
        }
        return new String(encode(raw.getBytes()));
    }

    /**
     * Decode a given base64 string (note this is dangerous if you call it when
     * the raw data is binary)
     *
     * @param encoded
     * @return
     * @throws Exception
     */
    public static String decode(String encoded) throws Exception {
        if ((encoded == null) || encoded.trim().length() == 0) {
            return encoded;
        }
        return new String(decode(encoded.getBytes()));
    }

    /**
     * Base64 encode an array of random bytes (can be used if its binary data)
     *
     * @param raw
     * @return
     * @throws Exception
     */
    public static byte[] encode(byte[] raw) throws Exception {
        if ((raw == null) || raw.length == 0) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Iterator<Triplet> it = new TripletIterator(raw);

        while (it.hasNext()) {
            Triplet t = it.next();
            baos.write(new Quartet(t).toByteArray());
        }
        return baos.toByteArray();
    }

    /**
     *
     * Base64 decode an array of bytes
     *
     * @param encoded
     * @return
     * @throws Exception
     */
    public static byte[] decode(byte[] encoded) throws Exception {
        if ((encoded == null) || encoded.length == 0) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Iterator<Quartet> it = new QuartetIterator(encoded);

        while (it.hasNext()) {
            Quartet q = it.next();
            baos.write(new Triplet(q).toByteArray());
        }

        return baos.toByteArray();
    }

    /*
     * Class that represents three bytes 
     * 
     */
    private static class Triplet {

        final Byte[] bytes = new Byte[3];
        Quartet q = null;

        Triplet(byte[] bytes, int startIndex) {
            int len = bytes.length;

            if (len > startIndex) {
                this.bytes[0] = bytes[startIndex];
            }
            if (len > startIndex + 1) {
                this.bytes[1] = bytes[startIndex + 1];
            }
            if (len > startIndex + 2) {
                this.bytes[2] = bytes[startIndex + 2];
            }
        }

        Triplet(Quartet q) {
            byte[] ba = q.toByteArray();

            bytes[0] = (byte) ((ba[0] << 2) | ((ba[1] >> 4) & 0x03));
            if (ba[2] == PLACE_HOLDER) {
                return;
            }

            bytes[1] = (byte) ((ba[1] & 0x0F) << 4 | ((ba[2] >> 2) & 0x0F));

            if (ba[3] == PLACE_HOLDER) {
                return;
            }
            bytes[2] = (byte) ((ba[2] & 0x03) << 6 | (ba[3] & 0x3F));
        }

        byte[] toByteArray() {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (int i = 0; i < bytes.length; i++) {
                if (bytes[i] != null) {
                    baos.write(bytes[i]);
                }
            }
            return baos.toByteArray();
        }
    }

    private static class TripletIterator implements Iterator<Triplet> {

        int numberOfTriplets = 0;
        byte[] bytes;
        int current = 0;

        TripletIterator(byte[] bytes) {
            this.bytes = bytes;
            numberOfTriplets = (int) Math.ceil(bytes.length / 3.0);
        }

        @Override
        public boolean hasNext() {
            return current < numberOfTriplets;
        }

        @Override
        public Triplet next() {
            return new Triplet(bytes, 3 * current++);
        }

        @Override
        public void remove() {
            throw new RuntimeException("Invalid operation");
        }

    }

    private static class Quartet {

        byte[] bytes = new byte[4];

        Quartet(byte[] bytes, int startIndex) {
            for (int i = 0; i < this.bytes.length; i++) {
                this.bytes[i] = '=';
            }

            if (bytes.length > startIndex) {
                this.bytes[0] = bytes[startIndex];
            }
            if (bytes.length > startIndex + 1) {
                this.bytes[1] = bytes[startIndex + 1];
            }
            if (bytes.length > startIndex + 2) {
                this.bytes[2] = bytes[startIndex + 2];
            }
            if (bytes.length > startIndex + 3) {
                this.bytes[3] = bytes[startIndex + 3];
            }
        }

        Quartet(Triplet t) {
            Byte a = t.bytes[0];
            Byte b = t.bytes[1];
            Byte c = t.bytes[2];

            if (a == null) {
                throw new RuntimeException("Invalid arguments");
            }

            if (b == null) {
                init(a);
                return;
            }
            if (c == null) {
                init(a, b);
                return;
            }
            init(a, b, c);
        }

        private void init(byte a, byte b, byte c) {
            bytes[0] = (byte) chars[(a >> 2) & 0x3F];
            bytes[1] = (byte) chars[((a & 0x03) << 4) | ((b >> 4) & 0x0F)];
            bytes[2] = (byte) chars[((b & 0x0F) << 2) | ((c >> 6) & 0x03)];
            bytes[3] = (byte) chars[c & 0x3F];
        }

        private void init(byte a, byte b) {
            bytes[0] = (byte) chars[(a >> 2) & 0x3F];
            bytes[1] = (byte) chars[((a & 0x03) << 4) | ((b >> 4) & 0x0F)];
            bytes[2] = (byte) chars[((b & 0x0F) << 2)];
            bytes[3] = (byte) '=';
        }

        private void init(byte a) {
            bytes[0] = (byte) chars[(a >> 2) & 0x3F];
            bytes[1] = (byte) chars[((a & 0x03) << 4)];
            bytes[2] = (byte) '=';
            bytes[3] = (byte) '=';
        }

        public byte[] toByteArray() {
            return bytes;
        }

        public String toString() {
            return new String(bytes);
        }
    }

    private static class QuartetIterator implements Iterator<Quartet> {

        byte[] bytes;
        int numberOfQuartets;
        int current = 0;

        QuartetIterator(byte[] raw) {
            bytes = new byte[raw.length];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = reverseMapChar(raw[i]);
            }
            if (bytes.length % 4 != 0) {
                throw new RuntimeException("Invalid Base64 string");
            }
            numberOfQuartets = bytes.length / 4;
        }

        @Override
        public boolean hasNext() {
            return current < numberOfQuartets;
        }

        @Override
        public Quartet next() {
            return new Quartet(bytes, 4 * current++);
        }

        @Override
        public void remove() {
            throw new RuntimeException("Invalid operation");

        }

        byte reverseMapChar(byte b) {
            if (b == '=') {
                return PLACE_HOLDER;
            }
            return reverseMap.get(b);
        }

    }
    private static final char[] chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
    private static final Map<Byte, Byte> reverseMap = new HashMap<Byte, Byte>();

    static {
        for (byte i = 0; i < chars.length; i++) {
            reverseMap.put((byte) chars[i], i);
        }
    }
}
