package com.only.redis.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RedisLock {

    /**
     * 特定参数识别，默认取第0个下标
     *
     * @return
     */
    int lockFiled() default 0;

    /**
     * 超时最大重试次数-默认3次
     *
     * @return
     */
    int maxAttempts() default 3;

    /**
     * 锁超时时间,单位：秒
     *
     * @return
     */
    long timeout() default 3;

}
