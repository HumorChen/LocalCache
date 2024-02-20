package cn.humorchen.localcache;

/**
 * @author  humorchen
 * date: 2023/12/29
 * description:信号量限速工具
 * 在n毫秒内只能获得limit信号量
 **/

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class SignalSpeedLimitUtil {
    /**
     * 信号量
     */
    private final AtomicInteger atomicInteger = new AtomicInteger(0);
    /**
     * 下次刷新时间
     */
    private volatile long nextLoadTime = System.currentTimeMillis();
    private final int UNLOCK = 0;
    private final int LOCK = 1;

    /**
     * 锁
     */
    private AtomicInteger lock = new AtomicInteger(0);

    /**
     * 统计时间周期
     */
    private final int timeGap;
    /**
     * 限制
     */
    private final int limit;


    /**
     * 创建限速工具
     *
     * @param timeGap n 毫秒内
     * @param limit   只释放limit个信号出去
     */
    public SignalSpeedLimitUtil(int timeGap, int limit) {
        this.timeGap = timeGap;
        this.limit = limit;
        if (timeGap < 10) {
            throw new IllegalArgumentException("时间范围不能小于10毫秒");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("限制个数不能小于1");
        }
        atomicInteger.set(limit);
    }

    /**
     * 尝试释放
     */
    private void tryRelease() {
        long now = System.currentTimeMillis();
        if (now > nextLoadTime) {
            // 尝试释放
            boolean compareAndSet = lock.compareAndSet(UNLOCK, LOCK);
            if (compareAndSet) {
                // 占领锁了
                try {
                    // 更新信号量
                    atomicInteger.set(limit);
                    // 更新下次时间
                    nextLoadTime = now + timeGap;
                } catch (Exception e) {
                    log.error("信号量释放报错", e);
                } finally {
                    // 释放锁
                    lock.compareAndSet(LOCK, UNLOCK);
                }
            }
        }
    }

    /**
     * 尝试一次获取信号
     *
     * @return
     */
    public boolean tryGetSignal() {
        tryRelease();
        return decrement();
    }

    /**
     * 自减
     *
     * @return
     */
    private boolean decrement() {
        int decrementAndGet = atomicInteger.decrementAndGet();
        if (decrementAndGet >= 0) {
            return true;
        }
        return false;
    }

    /**
     * 每次增长20%睡眠时间
     * 最多一次休眠一分钟
     *
     * @param sleep
     * @return
     */
    private int sleepAgain(int sleep) {
        return Math.min(sleep * 12 / 10, 60000);
    }

    /**
     * 获得信号
     *
     * @return
     */
    public boolean getSignal() {
        int sleep = 50;
        try {
            while (true) {
                tryRelease();
                boolean decrement = decrement();
                if (decrement) {
                    return true;
                }
                // 锁定失败了就休息
                TimeUnit.MILLISECONDS.sleep(sleep);
                // 下次休息时间增长
                sleep = sleepAgain(sleep);
            }
        } catch (Exception e) {
            log.error("获取信号报错", e);
        }
        return false;
    }

    /**
     * 在指定时间内获得信号
     *
     * @return
     */
    public boolean getSignal(int time, TimeUnit unit) {
        int sleep = 100;
        long endTime = System.currentTimeMillis() + unit.toMillis(time);
        try {
            while (System.currentTimeMillis() <= endTime) {
                tryRelease();
                boolean decrement = decrement();
                if (decrement) {
                    return true;
                }
                // 锁定失败了就休息
                TimeUnit.MILLISECONDS.sleep(sleep);
                // 下次休息时间增长
                sleep = sleepAgain(sleep);
            }
        } catch (Exception e) {
            log.error("获取信号报错", e);
        }
        return false;
    }
}
