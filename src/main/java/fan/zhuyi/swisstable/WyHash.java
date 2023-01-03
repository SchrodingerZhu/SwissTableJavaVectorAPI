package fan.zhuyi.swisstable;

import static java.lang.Math.unsignedMultiplyHigh;

/**
 * Modified from <a href="https://github.com/dynatrace-oss/hash4j">hash4j</a>
 */

public class WyHash {
    private final long seed;
    private final long[] secret;

    private WyHash(long seed, long[] secret) {
        this.seed = seed;
        this.secret = secret;
    }

    public long hashBytesToLong(byte[] input, int off, int len) {
        var seed = this.seed ^ wymix(this.seed ^ secret[0], secret[1]);
        long a = 0;
        long b = 0;
        if (len <= 16) {
            if (len >= 4) {
                a = (wyr4(input, off) << 32) | wyr4(input, off + ((len >> 3) << 2));
                b = (wyr4(input, off + len - 4) << 32) | wyr4(input, off + len - 4 - ((len >>> 3) << 2));
            } else if (len > 0) {
                a = wyr3(input, off, len);
            }
        } else {
            int i = len;
            int p = off;
            if (i > 48) {
                long s1 = seed;
                long s2 = seed;
                do {
                    seed = wymix(wyr8(input, p) ^ secret[1], wyr8(input, p + 8) ^ seed);
                    s1 = wymix(wyr8(input, p + 16) ^ secret[2], wyr8(input, p + 24) ^ s1);
                    s2 = wymix(wyr8(input, p + 32) ^ secret[3], wyr8(input, p + 40) ^ s2);
                    p += 48;
                    i -= 48;
                } while (i > 48);
                seed ^= s1 ^ s2;
            }
            while (i > 16) {
                seed = wymix(wyr8(input, p) ^ secret[1], wyr8(input, p + 8) ^ seed);
                i -= 16;
                p += 16;
            }
            a = wyr8(input, p + i - 16);
            b = wyr8(input, p + i - 8);
        }
        a ^= secret[1];
        b ^= seed;
        var x = a * b;
        var y = unsignedMultiplyHigh(a, b);
        return wymix(x ^ secret[0] ^ len, y ^ secret[1]);
    }

    private static long wymix(long a, long b) {
        long x = a * b;
        long y = unsignedMultiplyHigh(a, b);
        return x ^ y;
    }

    private static long wyr3(byte[] data, int off, int k) {
        var a = ((long) data[off]) << 16;
        var b = ((long) data[off + k / 2]) << 8;
        var c = ((long) data[off + k - 1]);
        return a | b | c;
    }

    private static long read(byte[] data, int p, int size) {
        long value = 0;
        for (int i = 0; i < size; ++i) {
            value |= ((long) data[p + i]) << (i * 8);
        }
        return value;
    }

    private static long wyr4(byte[] data, int p) {
        return read(data, p, 4);
    }

    private static long wyr8(byte[] data, int p) {
        return read(data, p, 8);
    }

    public static WyHash create(long seedForHash) {
        return new WyHash(seedForHash, DEFAULT_SECRET);
    }

    public static WyHash create(long seedForHash, long seedForInitialize) {
        long[] secret = makeSecret(seedForInitialize);
        return new WyHash(seedForHash, secret);
    }

    public static WyHash create(long seed, long[] secret) {
        return new WyHash(seed, secret);
    }

    private static final long[] DEFAULT_SECRET = {0xa0761d6478bd642fL, 0xe7037ed1a0b428dbL, 0x8ebc6af09c88c6e3L, 0x589965cc75374cc3L};

    private static long[] makeSecret(long seed) {
        long[] secret = new long[4];
        byte[] c = {(byte) 15, (byte) 23, (byte) 27, (byte) 29, (byte) 30, (byte) 39, (byte) 43, (byte) 45, (byte) 46, (byte) 51, (byte) 53, (byte) 54, (byte) 57, (byte) 58, (byte) 60, (byte) 71, (byte) 75, (byte) 77, (byte) 78, (byte) 83, (byte) 85, (byte) 86, (byte) 89, (byte) 90, (byte) 92, (byte) 99, (byte) 101, (byte) 102, (byte) 105, (byte) 106, (byte) 108, (byte) 113, (byte) 114, (byte) 116, (byte) 120, (byte) 135, (byte) 139, (byte) 141, (byte) 142, (byte) 147, (byte) 149, (byte) 150, (byte) 153, (byte) 154, (byte) 156, (byte) 163, (byte) 165, (byte) 166, (byte) 169, (byte) 170, (byte) 172, (byte) 177, (byte) 178, (byte) 180, (byte) 184, (byte) 195, (byte) 197, (byte) 198, (byte) 201, (byte) 202, (byte) 204, (byte) 209, (byte) 210, (byte) 212, (byte) 216, (byte) 225, (byte) 226, (byte) 228, (byte) 232, (byte) 240};
        for (int i = 0; i < 4; i++) {
            boolean ok;
            do {
                ok = true;
                seed += 0xa0761d6478bd642fL;
                secret[i] = (c[(int) (Long.remainderUnsigned(wymix(seed, seed ^ 0xe7037ed1a0b428dbL), c.length))] & 0xFFL);
                if ((secret[i] & 1) == 0) {
                    seed += 0x633acdbf4d2dbd49L; // = 7 * 0xa0761d6478bd642fL
                    ok = false;
                    continue;
                }
                for (int j = 8; j < 64; j += 8) {
                    seed += 0xa0761d6478bd642fL;
                    secret[i] |= (c[(int) (Long.remainderUnsigned(wymix(seed, seed ^ 0xe7037ed1a0b428dbL), c.length))] & 0xFFL) << j;
                }
                for (int j = 0; j < i; j++) {
                    if (Long.bitCount(secret[j] ^ secret[i]) != 32) {
                        ok = false;
                        break;
                    }
                }
            } while (!ok);
        }
        return secret;
    }

    public static final WyHash DEFAULT = create(0L);

    public Hasher<String> asStringHasher() {
        var base = this;
        return key -> {
            var bytes = key.getBytes();
            return base.hashBytesToLong(bytes, 0, bytes.length);
        };
    }

    public Hasher<byte[]> asByteArrayHasher() {
        var base = this;
        return bytes -> base.hashBytesToLong(bytes, 0, bytes.length);
    }

    public Hasher<Integer> asIntegerHasher() {
        var base = this;
        return key -> {
            byte[] bytes = {(byte) key.intValue(), (byte) (key >>> 8), (byte) (key >>> 16), (byte) (key >>> 24),};
            return base.hashBytesToLong(bytes, 0, 4);
        };
    }

    public Hasher<Long> asLongHasher() {
        var base = this;
        return key -> {
            byte[] bytes = {(byte) key.intValue(), (byte) (key >>> 8), (byte) (key >>> 16), (byte) (key >>> 24), (byte) (key >>> 32), (byte) (key >>> 40), (byte) (key >>> 48), (byte) (key >>> 56),};
            return base.hashBytesToLong(bytes, 0, 8);
        };
    }
}
