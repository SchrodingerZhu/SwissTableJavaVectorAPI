package fan.zhuyi.swisstable;

import static java.lang.Math.unsignedMultiplyHigh;
import static java.lang.reflect.Array.getInt;
import static java.lang.reflect.Array.getLong;

/**
 * Modified from <a href="https://github.com/dynatrace-oss/hash4j">hash4j</a>
 */

public class WyHash implements Hasher<String> {
    private final long seed;
    private final long secret1;
    private final long secret2;
    private final long secret3;

    private WyHash(long seed, long secret1, long secret2, long secret3) {
        this.seed = seed;
        this.secret1 = secret1;
        this.secret2 = secret2;
        this.secret3 = secret3;
    }

    public long hashBytesToLong(byte[] input, int off, int len) {
        var seed = wymix(this.seed ^ secret1, len ^ secret2);
        long see0 = seed;
        long a;
        long b;
        if (len <= 16) {
            if (len >= 4) {
                a = (wyr4(input, off) << 32) | wyr4(input, off + ((len >>> 3) << 2));
                b = (wyr4(input, off + len - 4) << 32) | wyr4(input, (off + len - 4) - ((len >>> 3) << 2));
            } else if (len > 0) {
                a = wyr3(input, off, len);
                b = 0;
            } else {
                a = 0;
                b = 0;
            }
        } else {
            int i = len;
            int p = off;
            long see1 = seed;
            long see2 = seed;
            while (i > 48) {
                see0 = wymix(getLong(input, p) ^ secret1, getLong(input, p + 8) ^ see0);
                see1 = wymix(getLong(input, p + 16) ^ secret2, getLong(input, p + 24) ^ see1);
                see2 = wymix(getLong(input, p + 32) ^ secret3, getLong(input, p + 40) ^ see2);
                p += 48;
                i -= 48;
            }
            see0 ^= see1 ^ see2;
            while (i > 16) {
                see0 = wymix(getLong(input, p) ^ secret1, getLong(input, p + 8) ^ see0);
                i -= 16;
                p += 16;
            }
            a = getLong(input, p + i - 16);
            b = getLong(input, p + i - 8);
        }
        a ^= secret2;
        b ^= seed;
        var x = a * b;
        var y = unsignedMultiplyHigh(a, b);
        return wymix(x ^ secret1 ^ len, y ^ secret2);
    }

    private static long wymix(long a, long b) {
        long x = a * b;
        long y = unsignedMultiplyHigh(a, b);
        return x ^ y;
    }

    private static long wyr3(byte[] data, int off, int k) {
        return ((data[off] & 0xFFL) << 16) | ((data[off + (k >>> 1)] & 0xFFL) << 8) | (data[off + k - 1] & 0xFFL);
    }

    private static long wyr4(byte[] data, int p) {
        return getInt(data, p) & 0xFFFFFFFFL;
    }

    public static WyHash create(long seedForHash) {
        long[] secret = DEFAULT_SECRET;
        long seed = seedForHash ^ secret[0];
        long secret1 = secret[1];
        long secret2 = secret[2];
        long secret3 = secret[3];
        return new WyHash(seed, secret1, secret2, secret3);
    }

    public static WyHash create(long seedForHash, long seedForInitialize) {
        long[] secret = makeSecret(seedForInitialize);
        long seed = seedForHash ^ secret[0];
        long secret1 = secret[1];
        long secret2 = secret[2];
        long secret3 = secret[3];
        return new WyHash(seed, secret1, secret2, secret3);
    }

    public static WyHash create(long seedForHash, long[] secret) {
        long seed = seedForHash ^ secret[0];
        long secret1 = secret[1];
        long secret2 = secret[2];
        long secret3 = secret[3];
        return new WyHash(seed, secret1, secret2, secret3);
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

    @Override
    public long hash(String key) {
        var bytes = key.getBytes();
        return hashBytesToLong(bytes, 0, bytes.length);
    }
}
