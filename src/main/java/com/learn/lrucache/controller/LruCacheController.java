package com.learn.lrucache.controller;

import com.learn.lrucache.bean.request.GetDataRequest;
import com.learn.lrucache.service.LruCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * @author Huangxuchu
 * @date 2021/1/12
 * @description 接口控制
 */
@Controller
@RequestMapping("/LruCache")
public class LruCacheController {
    private static final Logger logger = LogManager.getLogger(LruCacheController.class);

    @Resource
    LruCacheService lruCacheService;

    @ResponseBody
    @RequestMapping("getData")
    public String getData(@RequestBody GetDataRequest body) {
        logger.info("getData " + body.toString());


        return "";
    }
}
