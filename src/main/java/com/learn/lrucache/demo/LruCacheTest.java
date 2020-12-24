package com.learn.lrucache.demo;

import com.alibaba.fastjson.JSON;

import java.util.LinkedHashMap;

/**
 * @Author: Huangxuchu
 * @Date: 2020/12/15
 * @Version:
 * @Mender:
 * @Modify:
 * @Description:
 */
public class LruCacheTest {
    public static void main(String[] args) {
        stepLinkedHashMap(false);
        printLine();
        stepLinkedHashMap(true);
        printLine();

        step1WithLruCache();
        printLine();

        step2WithLruCache();
        printLine();
    }

    /**
     * 说明LinkedHashMap的排序方式
     *
     * @param accessOrder
     */
    public static void stepLinkedHashMap(boolean accessOrder) {
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<>(0, 0.75f, accessOrder);
        for (int i = 0; i < 4; i++) {
            linkedHashMap.put("k-" + i, "v-" + i);
        }
        System.out.println("初始化LinkedHashMap的数据：" + JSON.toJSONString(linkedHashMap));

        String v2 = linkedHashMap.get("k-2");
        System.out.println(String.format("当前accessOrder=%s时，执行了Get后LinkedHashMap的数据：%s", accessOrder, JSON.toJSONString(linkedHashMap)));
    }

    public static void step1WithLruCache() {
        LruCache<String, String> lruCache = new MLruCache(4);
        int index = 0;
        for (int i = 0; i < 4; i++) {
            lruCache.put("k-" + i, "v-" + i);
            index++;
        }
        System.out.println(String.format("LruCache添加数据%s条，数据：%s", index, lruCache.toString()));

        String v1 = lruCache.get("k-1");
        System.out.println(String.format("获取Key=k-1后，数据的排序：%s", lruCache.toString()));

        for (int i = index; i < 4 + 2; i++) {
            lruCache.put("k-" + i, "v-" + i);
            index++;
        }
        System.out.println(String.format("LruCache添加数据%s条，数据：%s", index, lruCache.toString()));
    }

    public static void step2WithLruCache() {
        LruCache<String, String> lruCache = new NLruCache(10);
        int index = 0;
        for (int i = 0; i < 4; i++) {
            lruCache.put("k-" + i, "v-" + i);
            index++;
        }
        System.out.println(String.format("LruCache添加数据%s条，数据：%s", index, lruCache.toString()));

        String v1 = lruCache.get("k-1");
        System.out.println(String.format("获取Key=k-1后，数据的排序：%s", lruCache.toString()));

        for (int i = index; i < 4 + 2; i++) {
            lruCache.put("k-" + i, "v-" + i);
            index++;
        }
        System.out.println(String.format("LruCache添加数据%s条，数据：%s", index, lruCache.toString()));
    }

    public static void printLine() {
        System.out.println("-----------------------------------------------\n");
    }

    public static class MLruCache extends LruCache<String, String> {

        /**
         * @param maxSize for caches that do not override {@link #sizeOf}, this is
         *                the maximum number of entries in the cache. For all other caches,
         *                this is the maximum sum of the sizes of the entries in this cache.
         */
        public MLruCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected int sizeOf(String key, String value) {
            return super.sizeOf(key, value);
        }

        /**
         * put()：evicted=false
         * get()：evicted=false
         * remove()：evicted=false
         * trimToSize()：evicted=true，说明这个key已经不符合缓存的要求，被逐出
         * 以上方法会调用此方法
         */
        @Override
        protected void entryRemoved(boolean evicted, String key, String oldValue, String newValue) {
            //System.out.println(String.format("Evicted(逐出)=%s Key=%s OldValue=%s NewValue=%s", evicted, key, oldValue, newValue));
        }
    }

    public static class NLruCache extends LruCache<String, String> {

        /**
         * @param maxSize for caches that do not override {@link #sizeOf}, this is
         *                the maximum number of entries in the cache. For all other caches,
         *                this is the maximum sum of the sizes of the entries in this cache.
         */
        public NLruCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected int sizeOf(String key, String value) {
            //return super.sizeOf(key, value);
            return value.length();
        }
    }
}
