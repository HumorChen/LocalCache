package cn.humorchen.localcache.interfaces.Impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.humorchen.localcache.interfaces.ICopierTest;
import cn.humorchen.localcache.interfaces.ILocalCacheResultCopier;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author  humorchen
 * date: 2024/1/24
 * description:
 **/
public class SetTypeTest implements ICopierTest {
    public static final SetTypeTest INSTANCE = new SetTypeTest();

    /**
     * 测试拷贝器是否正常
     *
     * @param copier
     * @return
     */
    @Override
    public boolean test(ILocalCacheResultCopier copier) {
        Set<Integer> set = CollectionUtil.newHashSet(1, 2, 3);
        Set<Integer> setCopy = (Set<Integer>) copier.copy(set);
        if (set == setCopy) {
            throw new RuntimeException("test error=set");
        }
        Iterator<Integer> setIterator = set.iterator();
        Iterator<Integer> setCopyIterator = setCopy.iterator();
        while (setIterator.hasNext()) {
            if (setIterator.next().intValue() != setCopyIterator.next().intValue()) {
                throw new RuntimeException("test error=Set element");
            }
        }

        Set<Test> testVoSet = new HashSet<>();
        Test testVo = new Test();
        testVo.setA(1);
        testVo.setB("1");
        testVoSet.add(testVo);
        Set<Test> testVoSetCopy = (Set<Test>) copier.copy(testVoSet);
        if (testVoSet == testVoSetCopy) {
            throw new RuntimeException("test error=Set testVoSetCopy");
        }
        Iterator<Test> testVoSetIterator = testVoSet.iterator();
        while (testVoSetIterator.hasNext()) {
            Test next = testVoSetIterator.next();
            Test nextCopy = testVoSetCopy.iterator().next();
            if (next.getA() != nextCopy.getA()) {
                throw new RuntimeException("test error=Set element testVoSetCopy a ");
            }
            if (!next.getB().equals(nextCopy.getB())) {
                throw new RuntimeException("test error=Set element testVoSetCopy b");
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
