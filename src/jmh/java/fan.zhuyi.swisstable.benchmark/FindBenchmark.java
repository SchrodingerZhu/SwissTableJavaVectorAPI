package fan.zhuyi.swisstable.benchmark;

import fan.zhuyi.swisstable.SwissTable;
import fan.zhuyi.swisstable.WyHash;
import org.openjdk.jmh.annotations.*;

import java.util.HashMap;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Fork(value = 1)
@Warmup(iterations = 5, time = 5)
public class FindBenchmark extends BenchmarkBase {
    SwissTable<String, Long> stringLongSwissTable;
    SwissTable<Long, Long> longLongSwissTable;
    HashMap<String, Long> stringLongHashMap;
    HashMap<Long, Long> longLongHashMap;

    @Setup
    public void initMaps() {
        stringLongSwissTable = new SwissTable<>(WyHash.DEFAULT.asStringHasher());
        longLongSwissTable = new SwissTable<>(x -> x);
        stringLongHashMap = new HashMap<>();
        longLongHashMap = new HashMap<>();
        for (int i = 0; i < keys.length; ++i) {
            stringLongSwissTable.insert(keys[i], values[i]);
            longLongSwissTable.insert(values[i], values[i]);
            stringLongHashMap.put(keys[i], values[i]);
            longLongHashMap.put(values[i], values[i]);
        }
    }

    @Benchmark
    public void swissTableFindExistingString() {
        for (String key : keys) {
            stringLongSwissTable.find(key);
        }
    }

    @Benchmark
    public void swissTableFindExistingLong() {
        for (long key : values) {
            longLongSwissTable.find(key);
        }
    }

    @Benchmark
    public void hashTableFindExistingString() {
        for (String key : keys) {
            stringLongHashMap.get(key);
        }
    }

    @Benchmark
    public void hashTableFindExistingLong() {
        for (long key : values) {
            longLongHashMap.get(key);
        }
    }

    @Benchmark
    public void swissTableFindMissingString() {
        for (long key : values) {
            stringLongSwissTable.find(Long.toString(key));
        }
    }

    @Benchmark
    public void swissTableFindMissingLong() {
        for (long key : values) {
            longLongSwissTable.find(-key);
        }
    }

    @Benchmark
    public void hashTableFindMissingString() {
        for (long key : values) {
            stringLongHashMap.get(Long.toString(key));
        }
    }

    @Benchmark
    public void hashTableFindMissingLong() {
        for (long key : values) {
            longLongHashMap.get(-key);
        }
    }
}