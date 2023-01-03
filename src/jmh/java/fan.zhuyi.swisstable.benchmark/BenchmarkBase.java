package fan.zhuyi.swisstable.benchmark;

import java.util.Random;


public abstract class BenchmarkBase {
    protected String[] keys;
    protected long[] values;

    protected BenchmarkBase() {
        var rand = new Random();
        keys = new String[100000];
        values = new long[100000];
        for (int i = 0; i < 100000; ++i) {
            long a = rand.nextLong(0, Long.MAX_VALUE), b = rand.nextLong(0, Long.MAX_VALUE), c = rand.nextLong(0, Long.MAX_VALUE);
            keys[i] = a + " " + b + " " + c;
            values[i] = a + b + c;
        }
    }
}
