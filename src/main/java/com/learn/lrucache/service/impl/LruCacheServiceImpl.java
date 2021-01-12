package com.learn.lrucache.service.impl;

import com.learn.lrucache.bean.HashLruCache;
import com.learn.lrucache.service.LruCacheService;
import org.springframework.stereotype.Service;

/**
 * @author Huangxuchu
 * @date 2021/1/12
 * @description
 */
@Service
public class LruCacheServiceImpl implements LruCacheService {
    private HashLruCache<String, Object> hashLruCache = new HashLruCache<>(30);

    @Override
    public String getData() {
        return null;
    }
}
