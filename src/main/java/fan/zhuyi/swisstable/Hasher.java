package fan.zhuyi.swisstable;

public interface Hasher<K> {
    long hash(K key);
}
