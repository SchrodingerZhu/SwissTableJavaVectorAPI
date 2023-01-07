package fan.zhuyi.swisstable.benchmark;

import fan.zhuyi.swisstable.GenericSwissTable;
import fan.zhuyi.swisstable.SwissTable;
import fan.zhuyi.swisstable.WyHash;
import org.openjdk.jmh.annotations.*;

import java.util.HashMap;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Fork(value = 1)
@Warmup(iterations = 5, time = 5)
public class FindBenchmark extends BenchmarkBase {
    SwissTable<StringWithHash, Long> stringLongSwissTable;
    SwissTable<Long, Long> longLongSwissTable;

    GenericSwissTable<StringWithHash, Long> stringLongGenericSwissTable;
    GenericSwissTable<Long, Long> longLongGenericSwissTable;
    HashMap<StringWithHash, Long> stringLongHashMap;
    HashMap<Long, Long> longLongHashMap;
    StringWithHash[] missingSequence;

    @Setup
    public void initMaps() {
        stringLongSwissTable = new SwissTable<>(StringWithHash::hash);
        longLongSwissTable = new SwissTable<>(x -> x);
        stringLongGenericSwissTable = new GenericSwissTable<>(StringWithHash::hash);
        longLongGenericSwissTable = new GenericSwissTable<>(x -> x);
        stringLongHashMap = new HashMap<>();
        longLongHashMap = new HashMap<>();
        missingSequence = new StringWithHash[keys.length];
        var hasher = WyHash.DEFAULT.asStringHasher();
        for (int i = 0; i < keys.length; ++i) {
            var missingKey = Long.toString(values[i]);
            missingSequence[i] = new StringWithHash(missingKey, hasher.hash(missingKey));
            stringLongGenericSwissTable.insert(keys[i], values[i]);
            longLongGenericSwissTable.insert(values[i], values[i]);
            stringLongSwissTable.insert(keys[i], values[i]);
            longLongSwissTable.insert(values[i], values[i]);
            stringLongHashMap.put(keys[i], values[i]);
            longLongHashMap.put(values[i], values[i]);
        }
    }

    @Benchmark
    public void genericSwissTableFindExistingString() {
        for (var key : keys) {
            stringLongGenericSwissTable.find(key);
        }
    }

    @Benchmark
    public void genericSwissTableFindExistingLong() {
        for (long key : values) {
            longLongGenericSwissTable.find(key);
        }
    }

    @Benchmark
    public void swissTableFindExistingString() {
        for (var key : keys) {
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
        for (var key : keys) {
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
    public void genericSwissTableFindMissingString() {
        for (var key : missingSequence) {
            stringLongGenericSwissTable.find(key);
        }
    }

    @Benchmark
    public void genericSwissTableFindMissingLong() {
        for (long key : values) {
            longLongGenericSwissTable.find(-key);
        }
    }

    @Benchmark
    public void swissTableFindMissingString() {
        for (var key : missingSequence) {
            stringLongSwissTable.find(key);
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
        for (var key : missingSequence) {
            stringLongHashMap.get(key);
        }
    }

    @Benchmark
    public void hashTableFindMissingLong() {
        for (long key : values) {
            longLongHashMap.get(-key);
        }
    }
}
