package com.learn.lrucache.bean;

import java.util.Arrays;

/**
 * @author Huangxuchu
 * @date 2021/1/11
 * @description
 */
public class HashLruCache<K, V> {
    public static final int DEFAULT_TABLE_SIZE = 16;
    public static final int DEFAULT_OVER_TIME = 1000 * 60 * 10;
    private Node<K, Entity<K, V>>[] table;

    /**
     * Size of this cache in units. Not necessarily the number of elements.
     * 以单位为单位的缓存的大小。不一定是元素的数量。
     */
    //当前缓存的大小
    private int size;
    //最大可缓存的大小
    private int maxSize;

    //put缓存的次数
    private int putCount;

    public HashLruCache(int maxSize) {
        this(maxSize, DEFAULT_TABLE_SIZE);
    }

    /**
     * @param maxSize   分片的最大存储数量
     * @param tableSize 分片数量
     */
    public HashLruCache(int maxSize, int tableSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        this.maxSize = maxSize;
        table = new Node[tableSize];
    }

    public final V put(K key, V value) {
        return put(key, value, DEFAULT_OVER_TIME);
    }

    /**
     * @param key
     * @param value
     * @param overTime
     * @return
     */
    public final V put(K key, V value, long overTime) {
        int hash = hash(key);
        int n = table.length;
        int i;

        Node<K, Entity<K, V>> p;
        Node<K, Entity<K, V>>[] tab = table;

        if ((p = tab[i = (n - 1) & hash]) == null) {
            // TODO: 2021/1/11 没有想到如何设置每个分片的最大数量，暂定每一个都是用户期望的大小
            p = tab[i] = new Node<>(0, maxSize);
        }

        putCount++;

        p.setUpdateTime(System.currentTimeMillis());
        Entity<K, V> entity = new Entity<>(hash, key, value, System.currentTimeMillis() + overTime);
        Entity<K, V> last = p.put(key, entity);

        return last == null ? null : last.value;
    }

    /**
     * @param key
     * @return
     */
    public final V get(K key) {
        int hash = hash(key);
        int n = table.length;
        int i;

        Node<K, Entity<K, V>> p;
        Node<K, Entity<K, V>>[] tab = table;

        if ((p = tab[i = (n - 1) & hash]) == null) {
            return null;
        }

        Entity<K, V> last = p.get(key);
        if (last != null) {
            // 修改节点的更新时间
            p.setUpdateTime(System.currentTimeMillis());

            if (System.currentTimeMillis() >= last.getOverTime()) {
                // 已经超过设定的过期时间，返回null
                return null;
            } else {
                return last.getValue();
            }
        } else {
            return null;
        }
    }

    public final V remove(K key) {
        int hash = hash(key);
        int n = table.length;
        int i;

        Node<K, Entity<K, V>> p;
        Node<K, Entity<K, V>>[] tab = table;

        if ((p = tab[i = (n - 1) & hash]) == null) {
            return null;
        }

        Entity<K, V> last = p.remove(key);
        if (last != null) {
            // 修改节点的更新时间
            p.setUpdateTime(System.currentTimeMillis());

            return last.getValue();
        } else {
            return null;
        }
    }

    static final int hash(Object key) {
        int h;
        return key == null ? 0 : (h = key.hashCode()) ^ h >>> 16;
    }

    private static class Entity<K, V> {
        private final int hash;
        private final K key;
        private V value;
        // 过期时间
        private long overTime;

        public Entity(int hash, K key, V value, long overTime) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.overTime = overTime;
        }

        public int getHash() {
            return hash;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public void setValue(V value) {
            this.value = value;
        }

        public long getOverTime() {
            return overTime;
        }

        public void setOverTime(long overTime) {
            this.overTime = overTime;
        }

        @Override
        public String toString() {
            return "Entity{" +
                    "hash=" + hash +
                    ", key=" + key +
                    ", value=" + value +
                    ", overTime=" + overTime +
                    '}';
        }
    }

    private static class Node<K, V> extends LruCache<K, V> {
        private long updateTime;

        public Node(int maxSize) {
            super(maxSize);
            this.updateTime = 0;
        }

        public Node(long updateTime, int maxSize) {
            super(maxSize);
            this.updateTime = updateTime;
        }

        public long getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(long updateTime) {
            this.updateTime = updateTime;
        }

    }

    @Override
    public String toString() {
        return "HashLruCache{" +
                "table=" + Arrays.toString(table) +
                ", maxSize=" + maxSize +
                ", putCount=" + putCount +
                '}';
    }
}
