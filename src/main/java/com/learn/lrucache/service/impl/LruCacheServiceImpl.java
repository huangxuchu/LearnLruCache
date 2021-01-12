package com.learn.lrucache.service.impl;

import com.learn.lrucache.bean.HashLruCache;
import com.learn.lrucache.service.LruCacheService;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * @author Huangxuchu
 * @date 2021/1/12
 * @description
 */
@Service
public class LruCacheServiceImpl implements LruCacheService {
    public static final String AREA_PATH = "json/area.json";

    private HashLruCache<String, Object> hashLruCache = new HashLruCache<>(30);

    @Override
    public String getData() {
        return null;
    }

    /**
     * 获取文件内容
     *
     * @return
     * @throws Exception
     */
    private String getJson() throws Exception {
        File file = ResourceUtils.getFile(AREA_PATH);
        InputStreamReader read = new InputStreamReader(new FileInputStream(file), "utf-8");
        try {
            //文件流是否存在； 不存在抛出异常信息
            if (file.isFile() && file.exists()) {
                BufferedReader bufferedReader = new BufferedReader(read);
                StringBuffer stringBuffer = new StringBuffer();
                String txt = null;
                //读取文件，将文件内容放入到set中
                while ((txt = bufferedReader.readLine()) != null) {
                    stringBuffer.append(txt);
                }
                return stringBuffer.toString();
            } else {
                throw new Exception("地区文件不存在！");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            read.close();     //关闭文件流
        }
    }
}
