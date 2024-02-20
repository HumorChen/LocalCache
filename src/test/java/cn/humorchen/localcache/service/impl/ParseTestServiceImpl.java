package cn.humorchen.localcache.service.impl;

import cn.humorchen.localcache.LocalCache;
import cn.humorchen.localcache.service.IParseTestService;
import cn.hutool.core.date.DateUtil;
import org.springframework.stereotype.Service;

/**
 * @author  chenfuxing
 * date: 2023/12/1
 * description:
 **/
@Service
public class ParseTestServiceImpl implements IParseTestService {
    /**
     * 转化
     *
     * @param id
     * @return
     */
    @Override
    @LocalCache(expireAfterWrite = 10)
    public long parse(String id) {
        return Long.parseLong(id);
    }

    /**
     * 说hi
     *
     * @param name
     * @return
     */
    @Override
    @LocalCache(expireAfterWrite = 20,refreshAfterWrite = 2)
    public String sayHi(String name) {
        return "Hi "+name+" "+ DateUtil.now();
    }
}
