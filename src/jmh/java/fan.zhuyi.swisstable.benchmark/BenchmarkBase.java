package fan.zhuyi.swisstable.benchmark;

import fan.zhuyi.swisstable.WyHash;

import java.util.Random;


public abstract class BenchmarkBase {
    record StringWithHash(String string, long hash) {
        @Override
        public int hashCode() {
            return (int) hash;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other instanceof StringWithHash self) return string.equals(self.string);
            return false;
        }
    }

    protected StringWithHash[] keys;
    protected long[] values;

    protected BenchmarkBase() {
        var rand = new Random();
        var hasher = WyHash.DEFAULT.asStringHasher();
        keys = new StringWithHash[100000];
        values = new long[100000];
        for (int i = 0; i < 100000; ++i) {
            long a = rand.nextLong(0, Long.MAX_VALUE), b = rand.nextLong(0, Long.MAX_VALUE), c = rand.nextLong(0, Long.MAX_VALUE);
            var string = a + " " + b + " " + c;
            var hash = hasher.hash(string);
            keys[i] = new StringWithHash(string, hash);
            values[i] = a + b + c;
        }
    }
}
