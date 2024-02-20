package cn.humorchen.localcache.interfaces;

/**
 * @author  humorchen
 * date: 2024/1/15
 * description: 本地缓存结果拷贝器接口
 **/
public interface ILocalCacheResultCopier {
    /**
     * 拷贝结果
     *
     * @param result
     * @return
     */
    Object copy(Object result);
}
