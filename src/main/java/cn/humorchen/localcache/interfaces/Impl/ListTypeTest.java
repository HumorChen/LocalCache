package cn.humorchen.localcache.interfaces.Impl;

import cn.humorchen.localcache.interfaces.ICopierTest;
import cn.humorchen.localcache.interfaces.ILocalCacheResultCopier;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author  humorchen
 * date: 2024/1/16
 * description:
 **/
@Slf4j
public class ListTypeTest implements ICopierTest {
    public static final ListTypeTest INSTANCE = new ListTypeTest();

    /**
     * 测试拷贝器是否正常
     *
     * @param copier
     * @return
     */
    @Override
    public boolean test(ILocalCacheResultCopier copier) {
        List<Integer> list = Arrays.asList(1, 2, 3);
        List<Integer> listCopy = (List<Integer>) copier.copy(list);
        if (list == listCopy) {
            throw new RuntimeException("test error=List");
        }
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).intValue() != listCopy.get(i).intValue()) {
                throw new RuntimeException("test error=List element");
            }
        }

        List<Test> testVoList = new ArrayList<>();
        Test testVo = new Test();
        testVo.setA(1);
        testVo.setB("1");
        testVoList.add(testVo);
        List<Test> testVoListCopy = (List<Test>) copier.copy(testVoList);
        if (testVoList == testVoListCopy) {
            throw new RuntimeException("test error=List testVoListCopy");
        }
        for (int i = 0; i < testVoList.size(); i++) {
            if (testVoList.get(i).getA() != testVoListCopy.get(i).getA()) {
                throw new RuntimeException("test error=List element testVoListCopy a ");
            }
            if (!testVoList.get(i).getB().equals(testVoListCopy.get(i).getB())) {
                throw new RuntimeException("test error=List element testVoListCopy b");
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
