package cam72cam.mod.world;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.concurrent.locks.StampedLock;

public class FastUtilMultiMap {
    private final Long2ObjectOpenHashMap<LongOpenHashSet> keyToValues
            = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<LongOpenHashSet> valueToKeys
            = new Long2ObjectOpenHashMap<>();
    private final StampedLock lock = new StampedLock();

    // 添加映射（允许多对多）
    public void put(long key, long value) {
        long stamp = lock.writeLock();
        try {
            // 添加 key -> value 映射
            keyToValues.computeIfAbsent(key, k -> new LongOpenHashSet()).add(value);
            // 添加 value -> key 映射
            valueToKeys.computeIfAbsent(value, v -> new LongOpenHashSet()).add(key);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    // 根据key获取values
    public LongOpenHashSet getValues(long key) {
        long stamp = lock.tryOptimisticRead();
        LongOpenHashSet set = keyToValues.get(key);
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                set = keyToValues.get(key);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return set != null ? set.clone() : new LongOpenHashSet();
    }

    // 根据value获取keys（重命名为复数形式）
    public LongOpenHashSet getKeys(long value) {
        long stamp = lock.tryOptimisticRead();
        LongOpenHashSet set = valueToKeys.get(value);
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                set = valueToKeys.get(value);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return set != null ? set.clone() : new LongOpenHashSet();
    }

    // 移除特定的key-value映射
    public boolean remove(long key, long value) {
        long stamp = lock.writeLock();
        try {
            boolean removed = false;

            // 从key->values中移除
            LongOpenHashSet values = keyToValues.get(key);
            if (values != null) {
                removed = values.remove(value);
                if (values.isEmpty()) {
                    keyToValues.remove(key);
                }
            }

            // 从value->keys中移除
            LongOpenHashSet keys = valueToKeys.get(value);
            if (keys != null) {
                keys.remove(key);
                if (keys.isEmpty()) {
                    valueToKeys.remove(value);
                }
            }

            return removed;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    // 根据key移除所有关联的value
    public LongOpenHashSet removeKey(long key) {
        long stamp = lock.writeLock();
        try {
            LongOpenHashSet values = keyToValues.remove(key);
            if (values == null) {
                return new LongOpenHashSet();
            }

            // 从所有相关的value->keys映射中移除这个key
            for (long value : values) {
                LongOpenHashSet keys = valueToKeys.get(value);
                if (keys != null) {
                    keys.remove(key);
                    if (keys.isEmpty()) {
                        valueToKeys.remove(value);
                    }
                }
            }

            return values;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    // 根据value移除所有关联的key
    public LongOpenHashSet removeValue(long value) {
        long stamp = lock.writeLock();
        try {
            LongOpenHashSet keys = valueToKeys.remove(value);
            if (keys == null) {
                return new LongOpenHashSet();
            }

            // 从所有相关的key->values映射中移除这个value
            for (long key : keys) {
                LongOpenHashSet values = keyToValues.get(key);
                if (values != null) {
                    values.remove(value);
                    if (values.isEmpty()) {
                        keyToValues.remove(key);
                    }
                }
            }

            return keys;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    // 清空所有映射
    public void clear() {
        long stamp = lock.writeLock();
        try {
            keyToValues.clear();
            valueToKeys.clear();
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    // 检查是否存在key-value映射
    public boolean contains(long key, long value) {
        long stamp = lock.tryOptimisticRead();
        LongOpenHashSet values = keyToValues.get(key);
        boolean contains = values != null && values.contains(value);
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                values = keyToValues.get(key);
                contains = values != null && values.contains(value);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return contains;
    }

    // 检查是否包含指定的key
    public boolean containsKey(long key) {
        long stamp = lock.tryOptimisticRead();
        boolean contains = keyToValues.containsKey(key);
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                contains = keyToValues.containsKey(key);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return contains;
    }

    // 检查是否包含指定的value
    public boolean containsValue(long value) {
        long stamp = lock.tryOptimisticRead();
        boolean contains = valueToKeys.containsKey(value);
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                contains = valueToKeys.containsKey(value);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return contains;
    }

    // 获取所有key的数量
    public int keySize() {
        long stamp = lock.tryOptimisticRead();
        int size = keyToValues.size();
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                size = keyToValues.size();
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return size;
    }

    // 获取所有value的数量
    public int valueSize() {
        long stamp = lock.tryOptimisticRead();
        int size = valueToKeys.size();
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                size = valueToKeys.size();
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return size;
    }

    // 获取所有key的集合（快照）
    public LongOpenHashSet keySet() {
        long stamp = lock.readLock();
        try {
            return new LongOpenHashSet(keyToValues.keySet());
        } finally {
            lock.unlockRead(stamp);
        }
    }

    // 获取所有value的集合（快照）
    public LongOpenHashSet valueSet() {
        long stamp = lock.readLock();
        try {
            return new LongOpenHashSet(valueToKeys.keySet());
        } finally {
            lock.unlockRead(stamp);
        }
    }

    // 获取映射的总数（所有key-value对的数量）
    public int totalSize() {
        long stamp = lock.readLock();
        try {
            int total = 0;
            for (LongOpenHashSet values : keyToValues.values()) {
                total += values.size();
            }
            return total;
        } finally {
            lock.unlockRead(stamp);
        }
    }
}