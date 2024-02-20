package cn.humorchen.localcache.interfaces;

/**
 * @author  humorchen
 * date: 2023/12/28
 * description: 缓存的参数对象对比接口
 **/
public interface ILocalCacheEquals {
    /**
     * 方法缓存参数对象自定义判定是否一致
     *
     * @param o
     * @return
     */
    default boolean equals(ILocalCacheEquals o) {
        return this == o;
    }
}
