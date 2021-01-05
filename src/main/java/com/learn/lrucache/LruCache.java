package com.learn.lrucache;/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.alibaba.fastjson.JSON;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Static library version of {@code android.util.LruCache}. Used to write apps
 * that run on API levels prior to 12. When running on API level 12 or above,
 * this implementation is still used; it does not try to switch to the
 * framework's implementation. See the framework SDK documentation for a class
 * overview.
 * <p>
 * By default, the cache size is measured in the number of entries. Override
 * {@link #sizeOf} to size the cache in different units. For example, this cache
 * is limited to 4MiB of bitmaps:
 * <pre>
 * {@code
 *   int cacheSize = 4 * 1024 * 1024; // 4MiB
 *   LruCache<String, Bitmap> bitmapCache = new LruCache<String, Bitmap>(cacheSize) {
 *       protected int sizeOf(String key, Bitmap value) {
 *           return value.getByteCount();
 *       }
 *   }
 * }
 * </pre>
 * <p>
 * LRU全称为Least Recently Used，即最近最少使用。
 * <p>
 * LruCache是Android 3.1的时候出现的，LruCache是一种缓存策略，持有的是强引用，但是会控制在一个峰值下。
 * 它内部维护了一个队列，每当从中取出一个值时，该值就移动到队列的尾部。当缓存已满而继续添加时，会将队列头部
 * 的值移除，方便GC。LruCache用于内存缓存，在避免程序发生OOM和提高执行效率有着良好表现。
 * - LruCache所占的内存大小是可以自定义的。
 * - LruCache的底层是通过LinkedHashMap实现数据缓存的。
 * - LruCache线程安全，LinkedHashMap非线程安全。
 * <p>
 * 《LruCache在美团DSP系统中的应用演进》：https://tech.meituan.com/2018/12/20/lrucache-practice-dsp.html
 * 《LinkedHashMap 源码详细分析（JDK1.8）》：http://www.tianxiaobo.com/2018/01/24/LinkedHashMap-%E6%BA%90%E7%A0%81%E8%AF%A6%E7%BB%86%E5%88%86%E6%9E%90%EF%BC%88JDK1-8%EF%BC%89/
 * 《Java集合之LinkedHashMap》：https://www.cnblogs.com/xiaoxi/p/6170590.html
 * 《LinkedHashMap原理和源码分析》：https://blog.csdn.net/qq_15893929/article/details/84798661
 * 《图解LinkedHashMap原理》：https://www.jianshu.com/p/8f4f58b4b8ab
 * 《LruCache 和 DiskLruCache 的使用以及原理分析》：LruCache 和 DiskLruCache 的使用以及原理分析
 */
public class LruCache<K, V> {
    // 容器 (每次访问一个元素（get或put），被访问的元素都被提到最后面去了)
    private final LinkedHashMap<K, V> map;

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
    //创建的次数，只有实现了create(key)方法后才会累加
    private int createCount;
    //逐出缓存的次数
    private int evictionCount;
    //命中缓存的次数
    private int hitCount;
    //丢失缓存的次数
    private int missCount;

    /**
     * @param maxSize for caches that do not override {@link #sizeOf}, this is
     *                the maximum number of entries in the cache. For all other caches,
     *                this is the maximum sum of the sizes of the entries in this cache.
     */
    public LruCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        this.maxSize = maxSize;

        /**
         * LinkedHashMap继承自HashMap，而且内部维护着一个双向队列，可以设置根据访问动作或者插入动作来调整顺序。
         * 我们根据访问动作会来调整顺序，当插入一个结点时候，将该结点插入到队列的尾部，或者，访问某个结点时，会将
         * 该结点调整到队列尾部。
         *
         * FROM：《阿里巴巴Java开发手册》
         * 9. 【推荐】集合初始化时，指定集合初始值大小。说明：HashMap 使用 HashMap(int initialCapacity) 初始化，
         * 正例：initialCapacity = (需要存储的元素个数 / 负载因子) + 1。注意负载因子（即loader factor）默认
         * 为 0.75，如果暂时无法确定初始值大小，请设置为 16（即 默认值）。
         * 反例：HashMap 需要放置 1024 个元素，由于没有设置容量初始大小，随着元素不断增加，容量 7 次被迫扩大，
         * resize 需要重建 hash 表，严重影响性能。
         *
         * @param  initialCapacity the initial capacity
         * @param  loadFactor      the load factor
         * @param  accessOrder     the ordering mode - <tt>true</tt> for access-order, <tt>false</tt> for insertion-order
         *                         排序模式，true表示在访问的时候进行排序( LruCache 核心工作原理就在此)，false表示在插入的时才排序。
         */
        this.map = new LinkedHashMap<K, V>(0, 0.75f, true);
    }

    /**
     * Caches {@code value} for {@code key}. The value is moved to the head of
     * the queue.
     * <p>
     * 1.异常判断说明LruCache不允许键或值为null的操作。
     * 2.在插入元素前会调用一次sizeOf，前面已经说过默认返回1，但一般我们会根据实际需要重写。
     * 比如用LruCache存储的value为File，那么sizeOf返回的就应该是当前对应该key的文件大小。
     * 3.相应的size也要完成自增长，因为当前缓存增加了，并且将对应的key-value插入到链表中。
     * 4.二次检查，如果该key已经存在链表中，此时新的value覆盖后，size要减去之前的value所占用的大小。
     * 5.上面的操作都是同步的，为了在多线程场景下保证size的准确性，否则缓存策略失效
     * 6.如果是覆盖了旧的value，LruCache对外提供了一个空方法entryRemoved。
     * 7.调用trimToSize,保证缓存不溢出。
     *
     * @return the previous value mapped by {@code key}.
     */
    @Nullable
    public final V put(@NonNull K key, @NonNull V value) {
        if (key == null || value == null) {
            throw new NullPointerException("key == null || value == null");
        }

        V previous;
        synchronized (this) {
            putCount++;
            // 这个方法返回的是1，也就是将缓存的个数加1.
            // 当缓存的是图片的时候，这个size应该表示图片占用的内存的大小，所以应该重写里面调用的sizeOf(key, value)方法
            size += safeSizeOf(key, value);
            previous = map.put(key, value); // 添加新的value，返回当前key的上一次保存的value
            if (previous != null) {
                size -= safeSizeOf(key, previous); // 减去旧的value的size
            }
        }

        if (previous != null) {
            entryRemoved(false, key, previous, value);
        }

        trimToSize(maxSize);
        return previous;
    }

    /**
     * Remove the eldest entries until the total of remaining entries is at or
     * below the requested size.
     * <p>
     * 该方法每插入一次元素就会被调用一次。整个方法就是一个无限循环，判断当前缓存大小不大于
     * 最大容量则结束循环。否则就取出LinkedHashMap的entrySet的头部，也就是最早被插入且
     * 最近未被访问过的键值对并删除，更新size。重复此步骤直到缓存<=最大容量。不得不说利用
     * 访问顺序的LinkedHashMap的特性完成LRU缓存，非常巧妙。
     * <p>
     * {@link #put(Object key, Object value)} 每插入一次元素就会被调用一次
     * {@link #resize(int maxSize)} 重设最大数量时调用
     * {@link #evictAll()} 逐出所有缓存时调用
     * {@link #get(Object key)} 当create(key)复写时且map.put()返回为空时执行
     *
     * @param maxSize the maximum size of the cache before returning. May be -1
     *                to evict even 0-sized elements.
     */
    public void trimToSize(int maxSize) {
        while (true) {
            K key;
            V value;
            synchronized (this) {
                if (size < 0 || (map.isEmpty() && size != 0)) {
                    throw new IllegalStateException(getClass().getName()
                            + ".sizeOf() is reporting inconsistent results!");
                }

                if (size <= maxSize || map.isEmpty()) {
                    break;
                }

                // 取出LinkedHashMap的entrySet的头部，最早被插入且最近未被访问过的键值对并删除
                Map.Entry<K, V> toEvict = map.entrySet().iterator().next();
                key = toEvict.getKey();
                value = toEvict.getValue();
                map.remove(key);
                size -= safeSizeOf(key, value);
                evictionCount++;
            }

            entryRemoved(true, key, value, null);
        }
    }

    /**
     * Returns the value for {@code key} if it exists in the cache or can be
     * created by {@code #create}. If a value was returned, it is moved to the
     * head of the queue. This returns null if a value is not cached and cannot
     * be created.
     */
    @Nullable
    public final V get(@NonNull K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        V mapValue;
        synchronized (this) {
            mapValue = map.get(key);
            if (mapValue != null) {
                hitCount++;
                return mapValue;
            }
            missCount++;
        }

        /*
         * Attempt to create a value. This may take a long time, and the map
         * may be different when create() returns. If a conflicting value was
         * added to the map while create() was working, we leave that value in
         * the map and release the created value.
         */
        // 默认是返回null，可以重写表示新建一个默认值
        V createdValue = create(key);
        if (createdValue == null) {
            return null;
        }

        synchronized (this) {
            createCount++;
            mapValue = map.put(key, createdValue);

            if (mapValue != null) {
                // There was a conflict so undo that last put
                // 说明插入的key有冲突了，需要撤销默认值，恢复插入原来的值mapValue
                map.put(key, mapValue);
            } else {
                size += safeSizeOf(key, createdValue);
            }
        }

        if (mapValue != null) {
            entryRemoved(false, key, createdValue, mapValue);
            return mapValue;
        } else {
            trimToSize(maxSize);
            return createdValue;
        }
    }

    /**
     * Sets the size of the cache.
     * 重置最大数量
     *
     * @param maxSize The new maximum size.
     */
    public void resize(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }

        synchronized (this) {
            this.maxSize = maxSize;
        }
        trimToSize(maxSize);
    }

    /**
     * Removes the entry for {@code key} if it exists.
     *
     * @return the previous value mapped by {@code key}.
     */
    @Nullable
    public final V remove(@NonNull K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        V previous;
        synchronized (this) {
            previous = map.remove(key);
            if (previous != null) {
                size -= safeSizeOf(key, previous);
            }
        }

        if (previous != null) {
            entryRemoved(false, key, previous, null);
        }

        return previous;
    }

    /**
     * Called for entries that have been evicted or removed. This method is
     * invoked when a value is evicted to make space, removed by a call to
     * {@link #remove}, or replaced by a call to {@link #put}. The default
     * implementation does nothing.
     *
     * <p>The method is called without synchronization: other threads may
     * access the cache while this method is executing.
     *
     * @param evicted  true if the entry is being removed to make space, false
     *                 if the removal was caused by a {@link #put} or {@link #remove}.
     * @param newValue the new value for {@code key}, if it exists. If non-null,
     *                 this removal was caused by a {@link #put}. Otherwise it was caused by
     *                 an eviction or a {@link #remove}.
     */
    protected void entryRemoved(boolean evicted, @NonNull K key, @NonNull V oldValue,
                                @Nullable V newValue) {
    }

    /**
     * Called after a cache miss to compute a value for the corresponding key.
     * Returns the computed value or null if no value can be computed. The
     * default implementation returns null.
     *
     * <p>The method is called without synchronization: other threads may
     * access the cache while this method is executing.
     *
     * <p>If a value for {@code key} exists in the cache when this method
     * returns, the created value will be released with {@link #entryRemoved}
     * and discarded. This can occur when multiple threads request the same key
     * at the same time (causing multiple values to be created), or when one
     * thread calls {@link #put} while another is creating a value for the same
     * key.
     */
    @Nullable
    protected V create(@NonNull K key) {
        return null;
    }

    private int safeSizeOf(K key, V value) {
        int result = sizeOf(key, value);
        if (result < 0) {
            throw new IllegalStateException("Negative size: " + key + "=" + value);
        }
        return result;
    }

    /**
     * Returns the size of the entry for {@code key} and {@code value} in
     * user-defined units.  The default implementation returns 1 so that size
     * is the number of entries and max size is the maximum number of entries.
     *
     * <p>An entry's size must not change while it is in the cache.
     */
    protected int sizeOf(@NonNull K key, @NonNull V value) {
        return 1;
    }

    /**
     * Clear the cache, calling {@link #entryRemoved} on each removed entry.
     */
    public final void evictAll() {
        trimToSize(-1); // -1 will evict 0-sized elements
    }

    /**
     * For caches that do not override {@link #sizeOf}, this returns the number
     * of entries in the cache. For all other caches, this returns the sum of
     * the sizes of the entries in this cache.
     */
    public synchronized final int size() {
        return size;
    }

    /**
     * For caches that do not override {@link #sizeOf}, this returns the maximum
     * number of entries in the cache. For all other caches, this returns the
     * maximum sum of the sizes of the entries in this cache.
     */
    public synchronized final int maxSize() {
        return maxSize;
    }

    /**
     * Returns the number of times {@link #get} returned a value that was
     * already present in the cache.
     */
    public synchronized final int hitCount() {
        return hitCount;
    }

    /**
     * Returns the number of times {@link #get} returned null or required a new
     * value to be created.
     */
    public synchronized final int missCount() {
        return missCount;
    }

    /**
     * Returns the number of times {@link #create(Object)} returned a value.
     */
    public synchronized final int createCount() {
        return createCount;
    }

    /**
     * Returns the number of times {@link #put} was called.
     */
    public synchronized final int putCount() {
        return putCount;
    }

    /**
     * Returns the number of values that have been evicted.
     */
    public synchronized final int evictionCount() {
        return evictionCount;
    }

    /**
     * Returns a copy of the current contents of the cache, ordered from least
     * recently accessed to most recently accessed.
     */
    public synchronized final Map<K, V> snapshot() {
        return new LinkedHashMap<K, V>(map);
    }

    @Override
    public synchronized final String toString() {
        int accesses = hitCount + missCount;
        int hitPercent = accesses != 0 ? (100 * hitCount / accesses) : 0;
        return String.format(Locale.US, "LruCache[maxSize=%d,hits=%d,misses=%d,hitRate=%d%%]; Data=%s",
                maxSize, hitCount, missCount, hitPercent, JSON.toJSONString(map));
    }
}
