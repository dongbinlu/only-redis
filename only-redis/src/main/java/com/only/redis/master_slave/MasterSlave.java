package com.only.redis.master_slave;

import com.only.redis.RedisApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RedisApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class MasterSlave {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Test
    public void set() {
        stringRedisTemplate.opsForValue().set("boy", "18");
    }

    @Test
    public void get(){
        String value = stringRedisTemplate.opsForValue().get("boy");
        System.out.println(value);
    }


}
