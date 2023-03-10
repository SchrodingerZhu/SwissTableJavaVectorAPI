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
public class InsertionBenchmark extends BenchmarkBase {
    @Benchmark
    public void genericSwissTableStringInsertion() {
        var table = new GenericSwissTable<StringWithHash, Long>(StringWithHash::hash);
        for (int i = 0; i < keys.length; i++) {
            table.insert(keys[i], values[i]);
        }
    }

    @Benchmark
    public void swissTableStringInsertion() {
        var table = new SwissTable<StringWithHash, Long>(StringWithHash::hash);
        for (int i = 0; i < keys.length; i++) {
            table.insert(keys[i], values[i]);
        }
    }

    @Benchmark
    public void hashMapStringInsertion() {
        var table = new HashMap<StringWithHash, Long>();
        for (int i = 0; i < keys.length; i++) {
            table.put(keys[i], values[i]);
        }
    }

    @Benchmark
    public void genericSwissTableLongInsertion() {
        var table = new GenericSwissTable<Long, Long>(x -> x);
        for (int i = 0; i < keys.length; i++) {
            table.insert(values[i], values[i]);
        }
    }

    @Benchmark
    public void swissTableLongInsertion() {
        var table = new SwissTable<Long, Long>(x -> x);
        for (int i = 0; i < keys.length; i++) {
            table.insert(values[i], values[i]);
        }
    }

    @Benchmark
    public void hashMapLongInsertion() {
        var table = new HashMap<Long, Long>();
        for (int i = 0; i < keys.length; i++) {
            table.put(values[i], values[i]);
        }
    }
}
