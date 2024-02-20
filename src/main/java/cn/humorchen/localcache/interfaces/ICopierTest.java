package cn.humorchen.localcache.interfaces;

/**
 * @author  humorchen
 * date: 2024/1/16
 * description: 拷贝器测试接口
 **/
public interface ICopierTest {
    /**
     * 测试拷贝器是否正常
     *
     * @param copier
     * @return
     */
    boolean test(ILocalCacheResultCopier copier);
}
