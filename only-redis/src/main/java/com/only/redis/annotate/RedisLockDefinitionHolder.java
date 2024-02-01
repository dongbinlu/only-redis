package com.only.redis.annotate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务队列保存参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisLockDefinitionHolder {

    /**
     * 业务唯一key
     */
    private String key;

    /**
     * 加锁时间-锁超时时间(秒 s)
     */
    private Long timeout;

    /**
     * 上次更新时间（毫秒 ms）
     */
    private Long lastModifyTime;


    /**
     * 保存当前线程
     */
    private Thread currentThread;

    /**
     * 超时最大重试次数
     */
    private int maxAttempts;

    /**
     * 当前尝试次数
     */
    private int currentCount;

    /**
     * 更新的时间周期（毫秒）
     * 公式 = 加锁时间（转成毫秒） / 3
     */
    private Long modifyPeriod;

    public RedisLockDefinitionHolder(String key, Long timeout, Long lastModifyTime, Thread currentThread, Integer maxAttempts) {
        this.key = key;
        this.timeout = timeout;
        this.lastModifyTime = lastModifyTime;
        this.currentThread = currentThread;
        this.maxAttempts = maxAttempts;
        this.modifyPeriod = timeout * 1000 / 3;
    }
}
