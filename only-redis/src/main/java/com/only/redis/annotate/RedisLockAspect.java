package com.only.redis.annotate;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
public class RedisLockAspect implements ApplicationRunner {

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 扫描的任务队列
     */
    private static ConcurrentLinkedQueue<RedisLockDefinitionHolder> linkedQueue = new ConcurrentLinkedQueue();

    private static final ScheduledExecutorService SCHEDULER = new ScheduledThreadPoolExecutor(1,
            new BasicThreadFactory.Builder().namingPattern("redisLock-schedule-pool").daemon(true).build());

    /**
     * @annotation 中的路径表示拦截特定注解
     */
    @Pointcut("@annotation(com.only.redis.annotate.RedisLock)")
    public void redisLockPC() {

    }

    @Around(value = "redisLockPC()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {

        Object target = pjp.getTarget();
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = target.getClass().getMethod(signature.getName(), signature.getParameterTypes());
        // 获取方法的参数
        Object[] param = pjp.getArgs();

        // 获取自定义注解
        RedisLock redisLock = method.getAnnotation(RedisLock.class);

        String key = param[redisLock.lockFiled()].toString();
        String value = UUID.randomUUID().toString();
        Object result = null;
        // 加锁
        try {

            Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, value, redisLock.timeout(), TimeUnit.SECONDS);
            if (!flag) {
                throw new Exception("You can't do it，because another has get the lock =-=");
            }
            Thread currentThread = Thread.currentThread();
            // 将本次Task信息加到【延时】队列中
            linkedQueue.add(new RedisLockDefinitionHolder(key,
                    redisLock.timeout(),
                    System.currentTimeMillis(),
                    currentThread,
                    redisLock.maxAttempts()));

            // 执行业务
            result = pjp.proceed();

            if (currentThread.isInterrupted()) {
                throw new InterruptedException("You had been interrupted =-=");
            }

        } catch (InterruptedException e) {
            log.error("Interrupt exception, rollback transaction", e);
            throw new Exception("Interrupt exception, please send request again");
        } catch (Throwable e) {
            log.error("has some error, please check again", e);
        } finally {
            // 请求结束后，强制删掉key，释放锁
            String afterValue = redisTemplate.opsForValue().get(key);
            if (StringUtils.equals(afterValue, value)) {
                redisTemplate.delete(key);
                log.info("release the lock, key is [" + key + "]");
            }
        }
        return result;
    }

    @PostConstruct
    public void init() {
        // do nothing
    }

    /**
     * 容器启动完毕执行
     *
     * @param args
     * @throws Exception
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        /**
         * 两秒执行一次【续时】操作
         * 提交完任务后延期0秒执行，每隔2秒执行一次
         * 执行周期固定，不管任务执行多长时间，每过2秒钟就会产生一个新的任务
         */

        SCHEDULER.scheduleAtFixedRate(() -> {
            // 这里用try-catch，否则报错后定时任务将不会再次执行
            try {
                Iterator<RedisLockDefinitionHolder> iterator = linkedQueue.iterator();
                while (iterator.hasNext()) {
                    RedisLockDefinitionHolder holder = iterator.next();
                    //判空
                    if (holder == null) {
                        iterator.remove();
                        continue;
                    }
                    // 判断key是否还有效，无效的话进行移除
                    if (redisTemplate.opsForValue().get(holder.getKey()) == null) {
                        iterator.remove();
                        continue;
                    }
                    // 超时重试次数，超过时给线程设定中断
                    if (holder.getCurrentCount() > holder.getMaxAttempts()) {
                        holder.getCurrentThread().interrupt();
                        iterator.remove();
                        continue;
                    }
                    // 判断是否进入最后三分之一时间
                    long curTime = System.currentTimeMillis();
                    boolean shouldExtend = (holder.getLastModifyTime() + holder.getModifyPeriod()) <= curTime;
                    if (shouldExtend) {
                        holder.setLastModifyTime(curTime);
                        redisTemplate.expire(holder.getKey(), holder.getTimeout(), TimeUnit.SECONDS);
                        log.info("key : [" + holder.getKey() + "], try count : " + holder.getCurrentCount());
                        holder.setCurrentCount(holder.getCurrentCount() + 1);
                    }
                }
            } catch (Exception e) {
                log.error("error while updating scheduler task", e);
            }
        }, 0, 2, TimeUnit.SECONDS);
    }
}
