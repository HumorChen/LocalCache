package cn.humorchen.localcache.enums;

import cn.humorchen.localcache.CopierTestUtil;
import cn.humorchen.localcache.interfaces.ILocalCacheResultCopier;
import cn.humorchen.localcache.interfaces.Impl.JsonSerializationCopier;
import cn.humorchen.localcache.interfaces.Impl.SpringBeanUtilCopier;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author  humorchen
 * date: 2024/1/15
 * description: 本地缓存结果拷贝策略枚举
 **/
@Getter
@Slf4j
public enum CopyResultStrategy {
    /**
     * 不拷贝结果
     */
    NONE("NONE", "无", null),
    /**
     * 使用spring bean utils拷贝结果（第一层深拷贝，嵌套的对象是浅拷贝）
     */
    SPRING_BEAN_UTILS("COPY_RESULT_USING_SPRING_BEAN_UTILS", "使用spring bean utils拷贝结果", SpringBeanUtilCopier.INSTANCE),
    /**
     * 使用json序列化拷贝结果（深拷贝）
     */
    JSON_SERIALIZATION("COPY_RESULT_USING_JSON_SERIALIZATION", "使用json序列化拷贝结果", JsonSerializationCopier.INSTANCE),
    ;
    /*
     * 编码
     */
    private final String code;
    /*
     * 描述
     */
    private final String desc;
    /*
     * 拷贝器
     * 允许自行覆盖copier，实现自定义的拷贝逻辑
     */
    private ILocalCacheResultCopier copier;

    CopyResultStrategy(String code, String desc, ILocalCacheResultCopier copier) {
        this.code = code;
        this.desc = desc;
        this.copier = copier;
    }

    /**
     * 根据code获取枚举
     *
     * @param code
     * @return
     */
    public static CopyResultStrategy getByCode(String code) {
        for (CopyResultStrategy value : CopyResultStrategy.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 覆盖拷贝器
     *
     * @param copier
     */
    public void setCopier(ILocalCacheResultCopier copier) {
        this.copier = copier;
        CopierTestUtil.testCopier(copier);
    }
}
