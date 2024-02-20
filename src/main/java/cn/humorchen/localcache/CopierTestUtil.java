package cn.humorchen.localcache;

import cn.humorchen.localcache.interfaces.ICopierTest;
import cn.humorchen.localcache.interfaces.ILocalCacheResultCopier;
import cn.humorchen.localcache.interfaces.Impl.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * @author  humorchen
 * date: 2024/1/16
 * description: 拷贝器测试工具类
 **/
@Slf4j
public class CopierTestUtil {
    private static final List<ICopierTest> testList = Arrays.asList(
            BasicTypeTest.INSTANCE,
            ListTypeTest.INSTANCE,
            MapTypeTest.INSTANCE,
            SetTypeTest.INSTANCE
    );

    /**
     * 添加拷贝器测试
     *
     * @param copierTest
     */
    public static void addCopierTest(ICopierTest copierTest) {
        testList.add(copierTest);
    }

    /**
     * 测试拷贝器
     *
     * @param copier
     * @return
     */
    public static boolean testCopier(ILocalCacheResultCopier copier) {
        for (ICopierTest copierTest : testList) {
            boolean tested = copierTest.test(copier);
            log.info("copierTest={},tested={}", copierTest.getClass().getSimpleName(), tested);
        }
        return true;
    }

    public static void main(String[] args) {
        testCopier(SpringBeanUtilCopier.INSTANCE);
        testCopier(JsonSerializationCopier.INSTANCE);
    }
}
