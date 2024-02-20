package cn.humorchen.localcache.interfaces.Impl;

import cn.humorchen.localcache.interfaces.ICopierTest;
import cn.humorchen.localcache.interfaces.ILocalCacheResultCopier;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @author  humorchen
 * date: 2024/1/16
 * description:
 **/
@Slf4j
public class MapTypeTest implements ICopierTest {
    public static final MapTypeTest INSTANCE = new MapTypeTest();

    /**
     * 测试拷贝器是否正常
     *
     * @param copier
     * @return
     */
    @Override
    public boolean test(ILocalCacheResultCopier copier) {
        // 测试map集合的拷贝
        Map<String, String> map = new HashMap<>();
        map.put("1", "1");
        Map<String, String> mapCopy = (Map<String, String>) copier.copy(map);
        if (map == mapCopy) {
            throw new RuntimeException("test error=Map");
        }
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!entry.getValue().equals(mapCopy.get(entry.getKey()))) {
                throw new RuntimeException("test error=Map element");
            }
        }

        Map<String, Test> testVoMap = new HashMap<>();
        Test testVo = new Test();
        testVo.setA(1);
        testVo.setB("1");
        testVoMap.put("1", testVo);
        Map<String, Test> testVoMapCopy = (Map<String, Test>) copier.copy(testVoMap);
        if (testVoMap == testVoMapCopy) {
            throw new RuntimeException("test error=Map testVoMapCopy");
        }
        for (Map.Entry<String, Test> entry : testVoMap.entrySet()) {
            if (entry.getValue().getA() != testVoMapCopy.get(entry.getKey()).getA()) {
                throw new RuntimeException("test error=Map element testVoMapCopy a ");
            }
            if (!entry.getValue().getB().equals(testVoMapCopy.get(entry.getKey()).getB())) {
                throw new RuntimeException("test error=Map element testVoMapCopy b");

            }
        }
        Map<Test, Test> testTestVoMap = new HashMap<>();
        testTestVoMap.put(testVo, testVo);
        Map<Test, Test> testTestVoMapCopy = (Map<Test, Test>) copier.copy(testTestVoMap);
        if (testTestVoMap == testTestVoMapCopy) {
            throw new RuntimeException("test error=Map testTestVoMapCopy");
        }
        for (Map.Entry<Test, Test> entry : testTestVoMapCopy.entrySet()) {
            if (entry.getKey() == entry.getValue()) {
                throw new RuntimeException("test error=Map element testTestVoMapCopy key and value");
            }

            if (entry.getValue().getA() != testTestVoMapCopy.get(entry.getKey()).getA()) {
                throw new RuntimeException("test error=Map element testTestVoMapCopy a ");
            }
            if (!entry.getValue().getB().equals(testTestVoMapCopy.get(entry.getKey()).getB())) {
                throw new RuntimeException("test error=Map element testTestVoMapCopy b");
            }
        }


        return true;
    }

    @Data
    @EqualsAndHashCode
    static class Test {
        private int a;
        private String b;
    }
}
