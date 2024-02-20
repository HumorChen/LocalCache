package cn.humorchen.localcache.enums;

import lombok.Getter;

/**
 * @author  humorchen
 * date: 2023/12/28
 * description: 清理策略枚举
 **/
@Getter
public enum CleanStrategyEnum {
    /**
     * 最久没有使用的优先淘汰
     */
    LRU("LRU", "淘汰最久没使用的数据"),
    /**
     * 大内存的优先淘汰调
     */
    SIZE_MAX_FIRST("SIZE_MAX_FIRST", "大内存优先淘汰"),
    /**
     * 随机淘汰
     */
    RANDOM("RANDOM", "随机淘汰"),

    NULL("NULL", "未选择"),
    ;

    CleanStrategyEnum(String code, String title) {
        this.code = code;
        this.title = title;
    }

    private final String code;
    private final String title;

    /**
     * of
     *
     * @param code
     * @return
     */
    public static CleanStrategyEnum of(String code) {
        for (CleanStrategyEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
