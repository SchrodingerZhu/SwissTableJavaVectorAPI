package fan.zhuyi.swisstable;

import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

@SuppressWarnings("unchecked")
public class GenericSwissTable<K, V> implements Serializable, Iterable<GenericSwissTable<K, V>.Entry> {
    @Serial
    private static final long serialVersionUID = -937785247107544233L;

    private static final byte EMPTY_BYTE = (byte) 0b11111111;
    private static final byte DELETED_BYTE = (byte) 0b10000000;
    private static final int VECTOR_LENGTH = 8;
    private final Hasher<K> hasher;
    private byte[] control;
    private Object[] keys;
    private Object[] values;
    private int bucketMask;
    private int items;
    private int growthLeft;

    public GenericSwissTable(Hasher<K> hasher) {
        this(hasher, 0);
    }

    public GenericSwissTable(Hasher<K> hasher, int capacity) {
        if (capacity == 0) {
            this.control = new byte[VECTOR_LENGTH];
            this.keys = null;
            this.values = null;
            this.bucketMask = 0;
            this.growthLeft = 0;
        } else {
            var buckets = Util.capacityToBuckets(capacity);
            this.control = new byte[VECTOR_LENGTH + buckets];
            this.keys = new Object[buckets];
            this.values = new Object[buckets];
            this.bucketMask = buckets - 1;
            this.growthLeft = Util.bucketMaskToCapacity(this.bucketMask);
        }
        this.items = 0;
        this.hasher = hasher;
        Arrays.fill(this.control, EMPTY_BYTE);
    }

    private static final long MSB_VECTOR = repeat((byte) 0x80);

    private long load(int offset) {
        return (control[offset] & 0xFFL) | ((control[offset + 1] & 0xFFL) << 8) | ((control[offset + 2] & 0xFFL) << 16) | ((control[offset + 3] & 0xFFL) << 24) | ((control[offset + 4] & 0xFFL) << 32) | ((control[offset + 5] & 0xFFL) << 40) | ((control[offset + 6] & 0xFFL) << 48) | ((control[offset + 7] & 0xFFL) << 56);
    }

    private void store(long data, int offset) {
        control[offset] = (byte) (data & 0xFF);
        control[offset + 1] = (byte) ((data >>> 8) & 0xFF);
        control[offset + 2] = (byte) ((data >>> 16) & 0xFF);
        control[offset + 3] = (byte) ((data >>> 24) & 0xFF);
        control[offset + 4] = (byte) ((data >>> 32) & 0xFF);
        control[offset + 5] = (byte) ((data >>> 40) & 0xFF);
        control[offset + 6] = (byte) ((data >>> 48) & 0xFF);
        control[offset + 7] = (byte) ((data >>> 56) & 0xFF);
    }

    private static long repeat(byte data) {
        long value = data & 0xFFL;
        return value  | (value << 8) | (value << 16) | (value << 24) | (value << 32) | (value << 40) | (value << 48) | (value << 56);
    }

    private static final long ONES_VECTOR = repeat((byte) 1);

    private static long valueInWord(long controlVector, long valueVector) {
        var cmp = controlVector ^ valueVector;
        return (cmp - ONES_VECTOR) & (~cmp) & MSB_VECTOR;
    }

    private long matchEmpty(int offset) {
        var vector = load(offset);
        return vector & (vector << 1) & MSB_VECTOR;
    }

    private long matchEmptyOrDeleted(int offset) {
        var vector = load(offset);
        return vector & MSB_VECTOR;
    }

    private long matchFull(int offset) {
        return matchEmpty(offset) ^ MSB_VECTOR;
    }

    private void convertSpecialToEmptyAndFullToDeleted(int offset) {
        var vector = load(offset);
        var full = (~vector) & MSB_VECTOR;
        store(~full + (full >>> 7), offset);
    }

    @NotNull
    @Override
    public Iterator<Entry> iterator() {
        return new TableIterator();
    }

    private int numOfBuckets() {
        return bucketMask + 1;
    }

    private void setControl(int index, byte value) {
        if (index < VECTOR_LENGTH) {
            int mirrorIndex = bucketMask + 1 + index;
            control[mirrorIndex] = value;
        }
        control[index] = value;
    }

    private static int firstTrue(long mask) {
        return Long.numberOfTrailingZeros(mask) / 8;
    }

    private int properInsertionSlot(int index) {
        if (Util.isFull(control[index])) return firstTrue(matchEmptyOrDeleted(0));
        return index;
    }

    private int findInsertSlot(long hash) {
        int position = Util.h1(hash) & bucketMask;
        int stride = 0;
        while (true) {
            var mask = matchEmptyOrDeleted(position);
            if (mask != 0) {
                var candidate = (position + firstTrue(mask)) & bucketMask;
                return properInsertionSlot(candidate);
            }
            stride += VECTOR_LENGTH;
            position = (position + stride) & bucketMask;
        }
    }

    private void setControlH2(int index, long hash) {
        setControl(index, Util.h2(hash));
    }

    private byte replaceControlH2(int index, long hash) {
        var prev = control[index];
        setControlH2(index, hash);
        return prev;
    }

    private void prepareRehashInPlace() {
        for (int i = 0; i < numOfBuckets(); i += VECTOR_LENGTH) {
            convertSpecialToEmptyAndFullToDeleted(i);
        }
        if (numOfBuckets() < VECTOR_LENGTH) {
            for (int i = 0; i < numOfBuckets(); ++i) {
                control[VECTOR_LENGTH + i] = control[i];
            }
        } else {
            for (int i = 0; i < VECTOR_LENGTH; ++i) {
                control[numOfBuckets() + i] = control[i];
            }
        }
    }

    private boolean isInTheSameGroup(int index, int new_index, long hash) {
        var probe_position = Util.h1(hash) & bucketMask;
        int x = ((index - probe_position) & bucketMask) / VECTOR_LENGTH;
        int y = ((new_index - probe_position) & bucketMask) / VECTOR_LENGTH;
        return x == y;
    }

    private void rehashInPlace() {
        prepareRehashInPlace();
        for (int idx = 0; idx < numOfBuckets(); ++idx) {
            if (control[idx] != DELETED_BYTE) {
                continue;
            }
            while (true) {
                var hash = hasher.hash((K) keys[idx]);
                var new_idx = findInsertSlot(hash);
                if (isInTheSameGroup(idx, new_idx, hash)) {
                    setControlH2(idx, hash);
                    break;
                }
                var prevControl = replaceControlH2(new_idx, hash);

                if (prevControl == EMPTY_BYTE) {
                    setControl(idx, EMPTY_BYTE);
                    keys[idx] = null;
                    values[idx] = null;
                    keys[new_idx] = keys[idx];
                    values[new_idx] = values[idx];
                    break;
                }

                var tmpKey = keys[new_idx];
                keys[new_idx] = keys[idx];
                keys[idx] = tmpKey;

                var tmpValue = values[new_idx];
                values[new_idx] = values[idx];
                values[idx] = tmpValue;
            }
        }

        growthLeft = Util.bucketMaskToCapacity(bucketMask) - items;
    }

    private GenericSwissTable<K, V> prepareResize(int capacity) {
        GenericSwissTable<K, V> newTable = new GenericSwissTable<>(hasher, capacity);
        newTable.growthLeft -= items;
        newTable.items += items;
        return newTable;
    }

    private boolean isBucketFull(int index) {
        return Util.isFull(control[index]);
    }

    private Slot prepareInsertSlot(long hash) {
        int index = findInsertSlot(hash);
        byte prevControl = control[index];
        setControlH2(index, hash);
        return new Slot(index, prevControl);
    }

    private void resize(int newCapacity) {
        var newTable = prepareResize(newCapacity);

        for (int i = 0; i < numOfBuckets(); ++i) {
            if (!isBucketFull(i)) continue;
            var hash = hasher.hash((K) keys[i]);
            var slot = newTable.prepareInsertSlot(hash);
            newTable.keys[slot.index] = keys[i];
            newTable.values[slot.index] = values[i];
        }

        this.keys = newTable.keys;
        this.values = newTable.values;
        this.control = newTable.control;
        this.bucketMask = newTable.bucketMask;
        this.items = newTable.items;
        this.growthLeft = newTable.growthLeft;
    }

    private void reserveWithRehash(int additional) {
        var newItems = items + additional;
        var fullCapacity = Util.bucketMaskToCapacity(bucketMask);
        if (newItems <= fullCapacity / 2) {
            rehashInPlace();
            return;
        }
        var newCapacity = Math.max(newItems, fullCapacity + 1);
        resize(newCapacity);
    }

    private int findWithHash(long hash, K key) {
        final byte h2 = Util.h2(hash); //highest 7 bits
        final long target = repeat(h2);
        int position = Util.h1(hash) & bucketMask; // h1 is just long to int
        int stride = 0;
        while (true) {
            final long vector = load(position);
            var mask = valueInWord(vector, target); // match byte is to load a vector of byte and do equality comparison
            while (MaskIterator.hasNext(mask)) {
                var bit = MaskIterator.getNext(mask);
                mask = MaskIterator.moveNext(mask);
                var index = (position + bit) & bucketMask;
                if (key.equals(keys[index])) return index;
            }

            if ((vector & (vector << 1) & MSB_VECTOR) != 0) {
                return -1;
            }

            stride += VECTOR_LENGTH;
            position = (position + stride) & bucketMask;
        }
    }

    private void recordAt(int index, byte prevControl, long hash) {
        growthLeft -= Util.specialIsEmpty(prevControl) ? 1 : 0;
        setControlH2(index, hash);
        items++;
    }

    private void insertAt(int index, long hash, K key, V value) {
        var prevControl = control[index];

        if (growthLeft == 0 && Util.specialIsEmpty(prevControl)) {
            reserveWithRehash(1);
            index = findInsertSlot(hash);
        }

        recordAt(index, prevControl, hash);
        keys[index] = key;
        values[index] = value;
    }

    public void insert(K key, V value) {
        var hash = hasher.hash(key);
        var index = findInsertSlot(hash);
        insertAt(index, hash, key, value);
    }

    public Optional<V> find(K key) {
        var hash = hasher.hash(key);
        var index = findWithHash(hash, key);
        if (index >= 0) return Optional.of((V) values[index]);
        return Optional.empty();
    }

    public Entry findEntry(K key) {
        var hash = hasher.hash(key);
        var index = findWithHash(hash, key);
        return new Entry(key, index);
    }

    public V findOrInsert(K key, V value) {
        var hash = hasher.hash(key);
        var index = findWithHash(hash, key);
        if (index >= 0) return (V) values[index];
        var slot = findInsertSlot(hash);
        insertAt(slot, hash, key, value);
        return (V) values[slot];
    }

    public Optional<V> erase(K key) {
        var entry = findEntry(key);
        if (entry.isOccupied()) return Optional.of(entry.erase());
        return Optional.empty();
    }

    public boolean containsKey(K key) {
        return findWithHash(hasher.hash(key), key) >= 0;
    }

    public void reserve(int additional) {
        reserveWithRehash(additional);
    }

    public int capacity() {
        return items + growthLeft;
    }

    public int growthLeft() {
        return growthLeft;
    }

    public int size() {
        return items;
    }

    public boolean isEmpty() {
        return items == 0;
    }

    public void rehash() {
        rehashInPlace();
    }

    private static class Slot {
        int index;
        byte prevControl;

        Slot(int index, byte prevControl) {
            this.index = index;
            this.prevControl = prevControl;
        }
    }

    private static class Util {
        public static boolean isFull(byte control) {
            return (control & 0x80) == 0;
        }

        public static boolean specialIsEmpty(byte control) {
            return (control & 0x10) != 0;
        }

        public static int h1(long hash) {
            return (int) hash;
        }

        public static byte h2(long hash) {
            return (byte) ((hash >>> (Long.SIZE - 7)) & 0x7f);
        }

        private static int nextPowerOfTwo(int value) {
            var idx = Long.numberOfLeadingZeros(value - 1);
            return 0x1 << (Integer.SIZE - idx);
        }

        private static int capacityToBuckets(int capacity) {
            if (capacity < 8) return (capacity < 4) ? 4 : 8;
            return nextPowerOfTwo(capacity * 8);
        }

        private static int bucketMaskToCapacity(int bucketMask) {
            if (bucketMask < 8) {
                return bucketMask;
            } else {
                return (bucketMask + 1) / 8 * 7;
            }
        }
    }

    private static class MaskIterator {
        public static boolean hasNext(long data) {
            return data != 0;
        }

        public static int getNext(long data) {
            return firstTrue(data);
        }

        public static long moveNext(long data) {
            return data & (data - 1);
        }
    }

    public class Entry {
        private final K key;
        private int index;

        private Entry(K key, int index) {
            this.key = key;
            this.index = index;
        }

        public K key() {
            return key;
        }

        public V value() throws NoSuchElementException {
            if (index >= 0) {
                return (V) values[index];
            }
            throw new NoSuchElementException("The entry is empty");
        }

        public boolean isOccupied() {
            return index >= 0;
        }

        public void set(V value) {
            if (index >= 0) {
                values[index] = value;
            } else {
                var hash = hasher.hash(key);
                var index = findInsertSlot(hash);
                insertAt(index, hash, key, value);
                this.index = index;
            }
        }

        public V erase() throws NoSuchElementException {
            if (index >= 0) {
                var indexBefore = (index - VECTOR_LENGTH) & bucketMask;
                var emptyBefore = matchEmpty(indexBefore);
                var emptyAfter = matchEmpty(index);
                byte ctrl;
                var leadingNonEmpty = Long.numberOfLeadingZeros(emptyBefore) / 8;
                var trailingNonEmpty = Long.numberOfTrailingZeros(emptyAfter) / 8;
                if (leadingNonEmpty + trailingNonEmpty >= VECTOR_LENGTH) {
                    ctrl = DELETED_BYTE;
                } else {
                    growthLeft++;
                    ctrl = EMPTY_BYTE;
                }
                setControl(index, ctrl);
                items--;
                var res = values[index];
                values[index] = null;
                keys[index] = null;
                return (V) res;
            }
            throw new NoSuchElementException("The entry is empty");
        }
    }

    public final class TableIterator implements Iterator<Entry> {

        long currentIterator;
        int offset;

        int remainingItems;

        private TableIterator() {
            this.currentIterator = matchFull(0);
            this.offset = 0;
            this.remainingItems = items;
        }

        private int moveNextUnchecked() {
            while (true) {
                if (MaskIterator.hasNext(currentIterator)) {
                    var result = offset + MaskIterator.getNext(currentIterator);
                    currentIterator = MaskIterator.moveNext(currentIterator);
                    return result;
                }
                var nextOffset = offset + VECTOR_LENGTH;
                currentIterator = matchFull(nextOffset);
                offset = nextOffset;
            }
        }

        @Override
        public boolean hasNext() {
            return remainingItems != 0;
        }


        @Override
        public @NotNull Entry next() {
            if (hasNext()) {
                var index = moveNextUnchecked() & bucketMask;
                remainingItems -= 1;
                return new Entry((K) keys[index], index);
            }
            throw new NoSuchElementException("Current Iterator has exhausted all elements in the table");
        }
    }
}