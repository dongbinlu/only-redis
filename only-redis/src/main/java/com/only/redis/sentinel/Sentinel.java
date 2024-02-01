package com.only.redis.sentinel;

import com.only.redis.RedisApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RedisApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Slf4j
public class Sentinel {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Test
    public void set() {
        stringRedisTemplate.opsForValue().set("boy", "22");
    }

    @Test
    public void get() {
        String value = stringRedisTemplate.opsForValue().get("boy");
        System.out.println(value);
    }

    @Test
    public void testSentinel() throws InterruptedException {
        int i = 1;
        while (true) {
            try {
                stringRedisTemplate.opsForValue().set("boy" + i, i + "");
                System.out.println("设置key：" + "boy" + i);
                i++;
                Thread.sleep(10000);
            } catch (Exception e) {
                log.error("错误：", e);
            }
        }
    }
}
