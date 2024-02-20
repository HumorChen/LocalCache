package cn.humorchen.localcache.interfaces.Impl;

import cn.humorchen.localcache.interfaces.ICopierTest;
import cn.humorchen.localcache.interfaces.ILocalCacheResultCopier;
import lombok.extern.slf4j.Slf4j;

/**
 * @author  humorchen
 * date: 2024/1/16
 * description:
 **/
@Slf4j
public class BasicTypeTest implements ICopierTest {
    public static final BasicTypeTest INSTANCE = new BasicTypeTest();

    /**
     * 测试拷贝器是否正常
     *
     * @param copier
     * @return
     */
    @Override
    public boolean test(ILocalCacheResultCopier copier) {
        int a = 1;
        int b = (int) copier.copy(a);
        if (a != b) {
            throw new RuntimeException("test error=int");
        }
        Integer c = 1;
        Integer d = (Integer) copier.copy(c);
        if (c.intValue() != d.intValue()) {
            throw new RuntimeException("test error=Integer");

        }
        char e = '1';
        char f = (char) copier.copy(e);
        if (e != f) {
            throw new RuntimeException("test error=char");
        }
        String s = "1";
        String copy = (String) copier.copy(s);
        if (!s.equals(copy)) {
            throw new RuntimeException("test error=String");
        }
        // 使用所有的基本数据类型测试拷贝器，如果拷贝器失败则返回false
        byte h = 1;
        byte g = (byte) copier.copy(h);
        if (h != g) {
            throw new RuntimeException("test error=byte");
        }

        return true;
    }
}
