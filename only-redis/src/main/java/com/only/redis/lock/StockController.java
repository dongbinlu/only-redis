package com.only.redis.lock;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/stock")
public class StockController {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @GetMapping("/deduct1")
    public String deductStock1() {

        synchronized (this) {
            int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("stock"));
            if (stock > 0) {
                int realStock = stock - 1;
                stringRedisTemplate.opsForValue().set("stock", realStock + "");
                System.out.println("扣减库存成功,剩余库存:" + realStock);
            } else {
                System.out.println("扣减失败,库存不足");
            }
        }
        return "end";
    }

    @GetMapping("/deduct2")
    public String deductStock2() {
        String lockKey = "product__001";
        String uuid = UUID.randomUUID().toString();

        try {
            Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, uuid, 10, TimeUnit.SECONDS);
            if (!flag) {
                return "系统繁忙中，请稍后再试";
            }

            int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("stock"));
            if (stock > 0) {
                stock--;
                stringRedisTemplate.opsForValue().set("stock", String.valueOf(stock));
                System.out.println("扣减成功，剩余库存：" + stock);
            } else {
                System.out.println("扣减失败，库存不足");
            }
        } finally {
            if (uuid.equals(stringRedisTemplate.opsForValue().get(lockKey))) {
                stringRedisTemplate.delete(lockKey);
            }
        }
        return "end";
    }

    @GetMapping("/deduct3")
    public String deductStock3() {
        String lockKey = "product_001";
        RLock lock = redissonClient.getLock(lockKey);
        try {
            lock.lock();
            int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("stock"));
            if (stock > 0) {
                stock--;
                stringRedisTemplate.opsForValue().set("stock", String.valueOf(stock));
                System.out.println("扣减成功，剩余库存：" + stock);
            } else {
                System.out.println("扣减失败，库存不足");
            }
        } finally {
            // 在解锁之前先判断要解锁的key是否已被锁定并且是否被当前线程保持。 如果满足条件时才解锁。 至此
            if (lock.isLocked()) {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
        return "end";
    }


}
