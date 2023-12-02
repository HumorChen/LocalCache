package cn.humorchen.LocalCache.service;

/**
 * @author: chenfuxing
 * date: 2023/12/1
 * description:
 **/
public interface IParseTestService {
    /**
     * 转化
     * @param id
     * @return
     */
    long parse(String id);

    /**
     * 说hi
     * @param name
     * @return
     */
    String sayHi(String name);
}
